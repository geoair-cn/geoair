package cn.geoair.map.tile.forge.fuser.mbtiles;

import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import org.sqlite.ProgressHandler;
import org.sqlite.SQLiteConnection;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** 按坐标读取源瓦片。全库统计和源文件写入不进入出图链路。 */

public class MbtilesSourceReader implements AutoCloseable {
    private final MbtilesReadOptions options;


    private static final int PROGRESS_OPCODES = 1000;
    // 先在 SQLite 中限制 BLOB 长度，避免异常瓦片被整块复制到 Java 堆。
    private static final String SELECT_TILE = "SELECT length(tile_data), CASE WHEN length(tile_data) <= ? THEN tile_data END "
            + "FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ? LIMIT 1";
    private static final String SELECT_SAMPLE = "SELECT zoom_level, tile_column, tile_row FROM tiles "
            + "WHERE zoom_level BETWEEN ? AND ? AND length(tile_data) BETWEEN 1 AND ? LIMIT 1";
    private final Map<Path, MbtilesReadPool> pools = new LinkedHashMap<>(8, 0.75f, true);
    private final ScheduledExecutorService reaper = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "mbtiles-source-idle");
        thread.setDaemon(true);
        return thread;
    });
    private boolean closed;

    public MbtilesSourceReader() { this(new MbtilesReadOptions()); }

    public MbtilesSourceReader(MbtilesReadOptions options) {
        this.options = options.copy();
        long interval = Math.min(options.getIdleMillis(), 30000L);
        reaper.scheduleWithFixedDelay(this::closeIdlePools, interval, interval, TimeUnit.MILLISECONDS);
    }

    public byte[] readTile(String source, int z, int x, int row) throws IOException, SQLException {
        return read(source, connection -> queryTile(connection, z, x, row));
    }

    /** 返回一张现有瓦片的坐标。Y 保留文件中的行号，调用方根据其行原点转换。 */
    public TileZxyApo sampleTile(String source, int minZoom, int maxZoom) throws IOException, SQLException {
        if (minZoom < 0 || maxZoom < minZoom) throw new IllegalArgumentException("MBTiles 取样级别范围无效");
        return read(source, connection -> {
            try (PreparedStatement query = connection.prepareStatement(SELECT_SAMPLE)) {
                query.setInt(1, minZoom);
                query.setInt(2, maxZoom);
                query.setInt(3, options.getMaxTileBytes());
                try (ResultSet tiles = query.executeQuery()) {
                    return tiles.next() ? TileZxyApo.of().setZ(tiles.getInt(1)).setX(tiles.getInt(2)).setY(tiles.getInt(3)) : null;
                }
            }
        });
    }

    private <T> T read(String source, SourceQuery<T> query) throws IOException, SQLException {
        Path path = Paths.get(source.trim()).toRealPath();
        TileOperationContext.checkCurrent();
        try (MbtilesAccessLease lease = MbtilesAccessCoordinator.operation(path)) {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            if (!attributes.isRegularFile()) throw new IOException("MBTiles 数据源必须是普通文件");
            MbtilesReadPool pool = borrow(path, attributes);
            try (Connection connection = pool.dataSource.getConnection()) {
                return queryWithDeadline(connection, query);
            } finally {
                release(pool);
            }
        } catch (InterruptedException stopped) {
            Thread.currentThread().interrupt(); throw new IOException("MBTiles读取已取消", stopped);
        }
    }

    private <T> T queryWithDeadline(Connection connection, SourceQuery<T> query) throws SQLException {
        SQLiteConnection sqlite = connection.unwrap(SQLiteConnection.class);
        TileOperationContext.checkCurrent();
        TileOperationContext context = TileOperationContext.current();
        long budget = options.getQueryTimeoutMillis();
        if (context != null) budget = Math.min(budget, context.remainingMillis());
        final long timeout = TimeUnit.MILLISECONDS.toNanos(budget);
        long started = System.nanoTime();
        Thread owner = Thread.currentThread();
        // 兼容旧库和 tiles 视图，遇到无索引等慢查询时限制 SQLite 虚拟机执行时间。
        ProgressHandler.setHandler(sqlite, PROGRESS_OPCODES, new ProgressHandler() {
            @Override protected int progress() {
                return owner.isInterrupted() || (context != null && context.getCancellation().isCancelled()) || System.nanoTime() - started >= timeout ? 1 : 0;
            }
        });
        try {
            return query.execute(connection);
        } finally {
            ProgressHandler.clearHandler(sqlite);
            TileOperationContext.checkCurrent();
        }
    }

    private interface SourceQuery<T> {
        T execute(Connection connection) throws SQLException;
    }

    private byte[] queryTile(Connection connection, int z, int x, int row) throws SQLException {
        int maxTileBytes = options.getMaxTileBytes();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_TILE)) {
            statement.setInt(1, maxTileBytes);
            statement.setInt(2, z);
            statement.setInt(3, x);
            statement.setInt(4, row);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                long size = result.getLong(1);
                if (size > maxTileBytes) throw new SQLException("MBTiles 瓦片超过读取大小限制");
                return size == 0 ? null : result.getBytes(2);
            }
        }
    }

    private synchronized MbtilesReadPool borrow(Path path, BasicFileAttributes attributes) throws SQLException {
        if (closed) throw new SQLException("MBTiles 读取服务已关闭");
        MbtilesReadPool pool = pools.get(path);
        if (pool != null && !pool.matches(attributes)) {
            // 同名文件被替换后，等旧查询归还连接，再从新文件建池。
            if (pool.readers != 0) throw new SQLException("MBTiles 文件正在切换，请稍后重试");
            pools.remove(path);
            pool.dataSource.close();
            pool = null;
        }
        if (pool == null) {
            if (pools.size() == options.getMaxOpenFiles() && !evictIdlePool()) {
                throw new SQLException("正在读取的 MBTiles 文件过多，请稍后重试");
            }
            pool = new MbtilesReadPool(path, attributes, options);
            pools.put(path, pool);
        }
        pool.readers++;
        return pool;
    }

    private synchronized void release(MbtilesReadPool pool) {
        pool.readers--;
        pool.lastUsedNanos = System.nanoTime();
        if (closed && pool.readers == 0) pool.dataSource.close();
    }

    private boolean evictIdlePool() {
        Iterator<MbtilesReadPool> iterator = pools.values().iterator();
        while (iterator.hasNext()) {
            MbtilesReadPool pool = iterator.next();
            if (pool.readers == 0) {
                iterator.remove();
                pool.dataSource.close();
                return true;
            }
        }
        return false;
    }

    private synchronized void closeIdlePools() {
        long now = System.nanoTime();
        Iterator<MbtilesReadPool> iterator = pools.values().iterator();
        while (iterator.hasNext()) {
            MbtilesReadPool pool = iterator.next();
            if (pool.readers == 0 && now - pool.lastUsedNanos >= TimeUnit.MILLISECONDS.toNanos(options.getIdleMillis())) {
                iterator.remove();
                pool.dataSource.close();
            }
        }
    }


    public synchronized void close() {
        closed = true;
        reaper.shutdownNow();
        for (MbtilesReadPool pool : pools.values()) {
            if (pool.readers == 0) pool.dataSource.close();
        }
        pools.clear();
    }
}
