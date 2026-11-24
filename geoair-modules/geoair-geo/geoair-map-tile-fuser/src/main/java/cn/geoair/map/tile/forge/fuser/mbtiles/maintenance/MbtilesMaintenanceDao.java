package cn.geoair.map.tile.forge.fuser.mbtiles.maintenance;

import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesAccessCoordinator;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesAccessLease;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesWriteCoordinator;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesWriteLease;
import cn.geoair.map.tile.forge.fuser.utils.TileResourceLimits;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.sqlite.ProgressHandler;
import org.sqlite.SQLiteConnection;
import org.sqlite.core.DB;

/** 外部缓存库的专用 DAO；SQL 仅使用固定表列、受控 PRAGMA 和绑定参数。 */
public class MbtilesMaintenanceDao {
    private static final int BUSY_TIMEOUT_MILLIS = 1000;
    private static final int QUERY_TIMEOUT_SECONDS = 5;
    private static final int MAX_KEYS = 128;
    private final String checkpointPrefix;

    public MbtilesMaintenanceDao() {
        this("geoair.transcode.");
    }

    public MbtilesMaintenanceDao(String checkpointPrefix) {
        if (checkpointPrefix == null || !checkpointPrefix.matches("[A-Za-z0-9_.-]{1,100}"))
            throw new IllegalArgumentException("恢复点命名空间无效");
        this.checkpointPrefix = checkpointPrefix;
    }

    private Connection open(Path file, boolean readOnly, BooleanSupplier cancelled)
            throws SQLException, IOException {
        TileOperationContext.checkCurrent();
        if (!Files.isRegularFile(file)) throw new IOException("缓存MBTiles文件不存在");
        String url = "jdbc:sqlite:" + file.toRealPath().toUri() + (readOnly ? "?mode=ro" : "");
        Connection db = DriverManager.getConnection(url);
        try {
            try (Statement sql = db.createStatement()) {
                sql.execute("PRAGMA busy_timeout=" + BUSY_TIMEOUT_MILLIS);
            }
            TileOperationContext context = TileOperationContext.current();
            final long deadline =
                    context == null
                            ? Long.MAX_VALUE
                            : System.nanoTime()
                                    + TimeUnit.MILLISECONDS.toNanos(context.remainingMillis());
            ProgressHandler.setHandler(
                    db.unwrap(SQLiteConnection.class),
                    1000,
                    new ProgressHandler() {
                        @Override
                        protected int progress() {
                            return cancelled.getAsBoolean()
                                            || Thread.currentThread().isInterrupted()
                                            || context != null
                                                    && (context.getCancellation().isCancelled()
                                                            || System.nanoTime() >= deadline)
                                    ? 1
                                    : 0;
                        }
                    });
            return db;
        } catch (SQLException | RuntimeException | Error failure) {
            try {
                db.close();
            } catch (SQLException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            throw failure;
        }
    }

    private long number(Connection db, String pragma) throws SQLException {
        try (Statement sql = db.createStatement();
                ResultSet row = sql.executeQuery("PRAGMA " + pragma)) {
            return row.next() ? row.getLong(1) : 0;
        }
    }

    private long size(Path path) throws IOException {
        return Files.exists(path) ? Files.size(path) : 0;
    }

    public MbtilesFileInfo inspect(Path file) throws Exception {
        try (MbtilesAccessLease lease = MbtilesAccessCoordinator.operation(file);
                Connection db = open(file, true, () -> false)) {
            MbtilesFileInfo info = new MbtilesFileInfo();
            info.setPath(file.toRealPath().toString());
            info.setMainBytes(size(file));
            info.setWalBytes(size(file.resolveSibling(file.getFileName() + "-wal")));
            info.setShmBytes(size(file.resolveSibling(file.getFileName() + "-shm")));
            info.setPageSize(number(db, "page_size"));
            info.setPageCount(number(db, "page_count"));
            info.setFreePages(number(db, "freelist_count"));
            info.setFreeBytes(Math.multiplyExact(info.getFreePages(), info.getPageSize()));
            info.setAutoVacuum((int) number(db, "auto_vacuum"));
            info.setIncrementalSupported(info.getAutoVacuum() == 2);
            try (Statement sql = db.createStatement();
                    ResultSet row = sql.executeQuery("PRAGMA journal_mode")) {
                info.setJournalMode(row.next() ? row.getString(1) : "unknown");
            }
            info.setDiskFreeBytes(Files.getFileStore(file).getUsableSpace());
            long required =
                    Math.addExact(Math.multiplyExact(info.getMainBytes(), 2), 64L * 1024 * 1024);
            info.setVacuumSpaceEnough(info.getDiskFreeBytes() >= required);
            return info;
        }
    }
    /** 要求可写的标准 tiles 表，唯一坐标索引用于有界游标；禁止在无索引大库上排序全表。 */
    public void validateTranscode(Path file) throws Exception {
        try (MbtilesAccessLease lease = MbtilesAccessCoordinator.operation(file);
                Connection db = open(file, true, () -> false)) {
            try (PreparedStatement sql =
                            db.prepareStatement(
                                    "SELECT type FROM sqlite_master WHERE name='tiles'");
                    ResultSet row = sql.executeQuery()) {
                if (!row.next() || !"table".equals(row.getString(1)))
                    throw new IllegalArgumentException("转码仅支持可写的标准tiles表");
            }
            try (PreparedStatement sql =
                    db.prepareStatement("SELECT name,value FROM metadata LIMIT 0")) {
                sql.executeQuery().close();
            }
            boolean indexed = false;
            try (Statement sql = db.createStatement();
                    ResultSet indexes = sql.executeQuery("PRAGMA index_list('tiles')")) {
                while (indexes.next()) {
                    if (indexes.getInt("unique") != 1 || indexes.getInt("partial") != 0) continue;
                    String name = indexes.getString("name").replace("'", "''");
                    List<String> columns = new ArrayList<>();
                    try (Statement detail = db.createStatement();
                            ResultSet fields =
                                    detail.executeQuery("PRAGMA index_info('" + name + "')")) {
                        while (fields.next()) columns.add(fields.getString("name"));
                    }
                    if (columns.equals(Arrays.asList("zoom_level", "tile_column", "tile_row")))
                        indexed = true;
                }
            }
            if (!indexed) throw new IllegalArgumentException("缓存库缺少唯一级别列行索引，不能安全执行大库转码");
        }
    }

    public List<MbtilesTileKey> keys(
            Path file,
            int min,
            int max,
            int zoom,
            int x,
            int row,
            MbtilesTileKey end,
            BooleanSupplier cancelled)
            throws Exception {
        List<MbtilesTileKey> keys = new ArrayList<>();
        try (MbtilesAccessLease lease = MbtilesAccessCoordinator.operation(file);
                Connection db = open(file, true, cancelled);
                PreparedStatement sql =
                        db.prepareStatement(
                                "SELECT zoom_level,tile_column,tile_row,length(tile_data) FROM tiles "
                                        + "WHERE zoom_level BETWEEN ? AND ? AND (zoom_level,tile_column,tile_row) > (?,?,?) "
                                        + "AND (zoom_level,tile_column,tile_row)<=(?,?,?) ORDER BY zoom_level,tile_column,tile_row LIMIT ?")) {
            sql.setInt(1, min);
            sql.setInt(2, max);
            sql.setInt(3, zoom);
            sql.setInt(4, x);
            sql.setInt(5, row);
            sql.setInt(6, end.getZoom());
            sql.setInt(7, end.getX());
            sql.setInt(8, end.getRow());
            sql.setInt(9, MAX_KEYS);
            sql.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            try (ResultSet rows = sql.executeQuery()) {
                while (rows.next())
                    keys.add(
                            new MbtilesTileKey(
                                    rows.getInt(1),
                                    rows.getInt(2),
                                    rows.getInt(3),
                                    rows.getLong(4)));
            }
        }
        return keys;
    }

    public MbtilesTileKey upperBound(Path file, int min, int max) throws Exception {
        try (MbtilesAccessLease lease = MbtilesAccessCoordinator.operation(file);
                Connection db = open(file, true, () -> false);
                PreparedStatement sql =
                        db.prepareStatement(
                                "SELECT zoom_level,tile_column,tile_row FROM tiles "
                                        + "WHERE zoom_level BETWEEN ? AND ? ORDER BY zoom_level DESC,tile_column DESC,tile_row DESC LIMIT 1")) {
            sql.setInt(1, min);
            sql.setInt(2, max);
            sql.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            try (ResultSet row = sql.executeQuery()) {
                return row.next()
                        ? new MbtilesTileKey(row.getInt(1), row.getInt(2), row.getInt(3), 0)
                        : null;
            }
        }
    }

    public byte[] readTile(Path file, MbtilesTileKey key, BooleanSupplier cancelled)
            throws Exception {
        try (MbtilesAccessLease lease = MbtilesAccessCoordinator.operation(file);
                Connection db = open(file, true, cancelled);
                PreparedStatement sql =
                        db.prepareStatement(
                                "SELECT CASE WHEN length(tile_data)<=? THEN tile_data END FROM tiles "
                                        + "WHERE zoom_level=? AND tile_column=? AND tile_row=?")) {
            sql.setInt(1, TileResourceLimits.getMaxTileBytes());
            sql.setInt(2, key.getZoom());
            sql.setInt(3, key.getX());
            sql.setInt(4, key.getRow());
            try (ResultSet row = sql.executeQuery()) {
                return row.next() ? row.getBytes(1) : null;
            }
        }
    }

    public String readCheckpoint(Path file, String id) throws Exception {
        try (MbtilesAccessLease lease = MbtilesAccessCoordinator.operation(file);
                Connection db = open(file, true, () -> false);
                PreparedStatement sql =
                        db.prepareStatement("SELECT value FROM metadata WHERE name=? LIMIT 1")) {
            sql.setString(1, checkpointPrefix + id);
            try (ResultSet row = sql.executeQuery()) {
                return row.next() ? row.getString(1) : null;
            }
        }
    }
    /** 瓦片更新和恢复点同库同事务提交；管理库进度晚写不会导致重启重复有损编码。 */
    public void commit(
            Path file,
            MbtilesTileKey key,
            byte[] expected,
            byte[] replacement,
            String id,
            String progress,
            BooleanSupplier cancelled)
            throws Exception {
        // 转码属于后台改写，已有在线请求/预缓存写入等待时先让其落盘。
        try (MbtilesWriteLease lease = MbtilesWriteCoordinator.statistics(file);
                Connection db = open(file, false, cancelled)) {
            db.setAutoCommit(false);
            try {
                if (cancelled.getAsBoolean()) throw new InterruptedException("转码已停止");
                if (replacement != null) {
                    try (PreparedStatement sql =
                            db.prepareStatement(
                                    "UPDATE tiles SET tile_data=? WHERE zoom_level=? "
                                            + "AND tile_column=? AND tile_row=? AND tile_data=?")) {
                        sql.setBytes(1, replacement);
                        sql.setInt(2, key.getZoom());
                        sql.setInt(3, key.getX());
                        sql.setInt(4, key.getRow());
                        sql.setBytes(5, expected);
                        if (sql.executeUpdate() != 1) throw new MbtilesTileConflictException();
                    }
                    metadata(db, "geoair.encoding", "per-tile");
                }
                metadata(db, checkpointPrefix + id, progress);
                if (cancelled.getAsBoolean()) throw new InterruptedException("转码已停止");
                db.commit();
            } catch (Exception failure) {
                db.rollback();
                throw failure;
            }
        }
    }

    private void metadata(Connection db, String key, String value) throws SQLException {
        try (PreparedStatement sql =
                db.prepareStatement("UPDATE metadata SET value=? WHERE name=?")) {
            sql.setString(1, value);
            sql.setString(2, key);
            if (sql.executeUpdate() != 0) return;
        }
        try (PreparedStatement sql =
                db.prepareStatement("INSERT INTO metadata(name,value) VALUES(?,?)")) {
            sql.setString(1, key);
            sql.setString(2, value);
            sql.executeUpdate();
        }
    }
    /** 私有标记说明逐片识别；保留历史format值，避免把混合库谎报为全JPEG。 */
    public void markPerTileEncoding(Path file) throws Exception {
        try (MbtilesWriteLease lease = MbtilesWriteCoordinator.write(file);
                Connection db = open(file, false, () -> false)) {
            metadata(db, "geoair.encoding", "per-tile");
        }
    }

    public int[] checkpoint(Path file, MbtilesCheckpointMode mode, BooleanSupplier cancelled)
            throws Exception {
        try (MbtilesAccessLease lease = MbtilesAccessCoordinator.operation(file);
                Connection db = open(file, false, cancelled);
                Statement sql = db.createStatement();
                ResultSet row = sql.executeQuery("PRAGMA wal_checkpoint(" + mode.name() + ")")) {
            if (!row.next()) throw new SQLException("WAL合并未返回状态");
            return new int[] {row.getInt(1), row.getInt(2), row.getInt(3)};
        }
    }

    public long incremental(Path file, int pages, BooleanSupplier cancelled) throws Exception {
        if (pages <= 0 || pages > 4096) throw new IllegalArgumentException("增量回收批次无效");
        try (MbtilesWriteLease lease = MbtilesWriteCoordinator.statistics(file);
                Connection db = open(file, false, cancelled)) {
            if (number(db, "auto_vacuum") != 2) throw new IllegalArgumentException("该库未启用增量回收");
            // 3.45驱动把零列PRAGMA当作更新，Statement.execute只前进一页；原生exec消费到DONE。
            // 仍复用此连接的busy_timeout和取消处理器，每批最多回收受限页数。
            DB sqlite = db.unwrap(SQLiteConnection.class).getDatabase();
            long before = number(db, "freelist_count");
            int status = sqlite._exec("PRAGMA incremental_vacuum(" + pages + ")");
            if (status != 0) sqlite.throwex(status);
            return Math.max(0, before - number(db, "freelist_count"));
        }
    }
    /** 调用方必须持有独占维护租约，并在执行前重新检查空间。 */
    public void vacuum(Path file, BooleanSupplier cancelled) throws Exception {
        MbtilesAccessCoordinator.requireMaintenance(file);
        try (Connection db = open(file, false, cancelled);
                Statement sql = db.createStatement()) {
            sql.execute("VACUUM");
        }
    }
}
