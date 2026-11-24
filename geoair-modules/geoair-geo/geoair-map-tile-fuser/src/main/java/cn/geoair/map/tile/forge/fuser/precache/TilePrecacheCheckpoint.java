package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingState;
import cn.geoair.map.tile.forge.fuser.image.TileImageWriteState;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 单个预缓存计划的本地 SQLite 检查点。每次提交同时保存进度区间、计数和失败坐标。 */
public final class TilePrecacheCheckpoint implements AutoCloseable {
    private final Connection connection;
    private final Path lockFile;
    private FileChannel taskLock;
    private FileLock taskLease;

    public TilePrecacheCheckpoint(Path file) throws IOException {
        Path database = file.toAbsolutePath().normalize();
        lockFile = database.resolveSibling(database.getFileName() + ".lock");
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + database.toUri());
            try (Statement sql = connection.createStatement()) {
                sql.execute("PRAGMA busy_timeout=5000");
                sql.execute("PRAGMA synchronous=FULL");
                sql.execute("CREATE TABLE IF NOT EXISTS checkpoint_identity (id INTEGER PRIMARY KEY CHECK(id=1), value TEXT NOT NULL)");
                sql.execute("CREATE TABLE IF NOT EXISTS checkpoint_batch (z INTEGER, first_pos INTEGER, last_pos INTEGER, PRIMARY KEY(z,first_pos))");
                sql.execute("CREATE TABLE IF NOT EXISTS checkpoint_count (z INTEGER, bucket INTEGER, status TEXT, n INTEGER NOT NULL, PRIMARY KEY(z,bucket,status))");
                sql.execute("CREATE TABLE IF NOT EXISTS checkpoint_failure (z INTEGER, pos INTEGER, x INTEGER, y INTEGER, status TEXT NOT NULL, PRIMARY KEY(z,pos))");
                sql.execute("CREATE INDEX IF NOT EXISTS checkpoint_failure_status ON checkpoint_failure(z,status,pos)");
            } catch (SQLException failure) {
                try { connection.close(); } catch (SQLException close) { failure.addSuppressed(close); }
                throw failure;
            }
        } catch (SQLException failure) {
            throw new IOException("无法打开瓦片检查点", failure);
        }
    }

    synchronized void begin(String identity, boolean retryOnly) throws SQLException {
        Objects.requireNonNull(identity, "任务和缓存身份不能为空");
        if (taskLock != null) throw new IllegalStateException("检查点已有任务正在执行");
        try {
            taskLock = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            taskLease = taskLock.tryLock();
            if (taskLease == null) throw new IllegalStateException("检查点已有任务正在执行");
            validateIdentity(identity, retryOnly);
        } catch (IOException | OverlappingFileLockException failure) {
            end();
            throw new IllegalStateException("无法锁定预缓存检查点，可能已有任务正在执行", failure);
        } catch (SQLException | RuntimeException failure) {
            end();
            throw failure;
        }
    }

    private void validateIdentity(String identity, boolean retryOnly) throws SQLException {
        if (!retryOnly) {
            try (PreparedStatement sql = connection.prepareStatement(
                    "INSERT OR IGNORE INTO checkpoint_identity(id,value) VALUES(1,?)")) {
                sql.setString(1, identity);
                sql.executeUpdate();
            }
        }
        try (Statement sql = connection.createStatement();
                ResultSet result = sql.executeQuery("SELECT value FROM checkpoint_identity WHERE id=1")) {
            if (!result.next() || !identity.equals(result.getString(1)))
                throw new IllegalStateException("预缓存计划或缓存身份已变化，无法复用检查点");
        }
    }

    synchronized void end() {
        if (taskLock == null) return;
        try {
            taskLock.close();
        } catch (IOException failure) {
            throw new IllegalStateException("释放预缓存检查点锁失败", failure);
        } finally {
            taskLease = null;
            taskLock = null;
        }
    }

    synchronized long completedTo(int z, long from) throws SQLException {
        try (PreparedStatement sql = connection.prepareStatement(
                "SELECT last_pos FROM checkpoint_batch WHERE z=? AND first_pos<=? ORDER BY first_pos DESC LIMIT 1")) {
            sql.setInt(1, z);
            sql.setLong(2, from);
            try (ResultSet result = sql.executeQuery()) { return result.next() ? Math.max(from, result.getLong(1)) : from; }
        }
    }

    synchronized long gapEnd(int z, long from, long limit) throws SQLException {
        try (PreparedStatement sql = connection.prepareStatement(
                "SELECT first_pos FROM checkpoint_batch WHERE z=? AND first_pos>? AND first_pos<? ORDER BY first_pos LIMIT 1")) {
            sql.setInt(1, z);
            sql.setLong(2, from);
            sql.setLong(3, limit);
            try (ResultSet result = sql.executeQuery()) { return result.next() ? result.getLong(1) : limit; }
        }
    }

    /** 按偏移量读取固定数量的失败坐标，调用方可逐页展示或重放。 */
    public synchronized List<TilePrecacheFailure> failures(int z, long after, int limit) throws SQLException {
        return failures(z, after, limit, null);
    }

    /** 按级别、可选失败原因及偏移游标读取。 */
    public synchronized List<TilePrecacheFailure> failures(int z, long after, int limit, TileOperationStatus status)
            throws SQLException {
        if (limit <= 0) throw new IllegalArgumentException("失败瓦片读取数量必须大于零");
        String filter = status == null ? "" : " AND status=?";
        try (PreparedStatement sql = connection.prepareStatement("SELECT pos,x,y,status FROM checkpoint_failure "
                + "WHERE z=? AND pos>?" + filter + " ORDER BY pos LIMIT ?")) {
            sql.setInt(1, z);
            sql.setLong(2, after);
            if (status != null) sql.setString(3, status.name());
            sql.setInt(status == null ? 3 : 4, limit);
            List<TilePrecacheFailure> failures = new ArrayList<>();
            try (ResultSet result = sql.executeQuery()) {
                while (result.next())
                    failures.add(new TilePrecacheFailure(result.getLong(1), result.getInt(2), result.getInt(3),
                            TileOperationStatus.valueOf(result.getString(4))));
            }
            return failures;
        }
    }

    synchronized void commit(int z, long from, long to, TilePrecacheProgress delta,
            List<TilePrecacheFailure> failures, List<TilePrecacheFailure> previous) throws SQLException {
        connection.setAutoCommit(false);
        try {
            if (previous.isEmpty()) mergeCompleted(z, from, to);
            EnumMap<TileOperationStatus, Long> counts = new EnumMap<>(TileOperationStatus.class);
            counts.putAll(delta.getCounts());
            if (!previous.isEmpty()) {
                try (PreparedStatement sql = connection.prepareStatement("DELETE FROM checkpoint_failure WHERE z=? AND pos=?")) {
                    for (TilePrecacheFailure failure : previous) {
                        counts.merge(failure.getStatus(), -1L, Long::sum);
                        sql.setInt(1, z);
                        sql.setLong(2, failure.getPosition());
                        sql.addBatch();
                    }
                    sql.executeBatch();
                }
            }
            addCounts(z, 0, counts);
            // 阶段计数表示实际处理次数，重放产生的读取和处理也计入实际工作量。
            addCounts(z, 1, delta.getStages().getLoads());
            addCounts(z, 2, delta.getStages().getProcessing());
            addCounts(z, 3, delta.getStages().getWrites());
            try (PreparedStatement sql = connection.prepareStatement("INSERT INTO checkpoint_failure VALUES(?,?,?,?,?)")) {
                for (TilePrecacheFailure failure : failures) {
                    sql.setInt(1, z);
                    sql.setLong(2, failure.getPosition());
                    sql.setInt(3, failure.getX());
                    sql.setInt(4, failure.getY());
                    sql.setString(5, failure.getStatus().name());
                    sql.addBatch();
                }
                sql.executeBatch();
            }
            connection.commit();
        } catch (SQLException | RuntimeException failure) {
            try { connection.rollback(); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private void mergeCompleted(int zoom, long from, long to) throws SQLException {
        long first = from, last = to;
        // 并发批次可能乱序完成；合并相邻区间，恢复时直接跳过大块已完成范围。
        String contact = " WHERE z=? AND first_pos<=? AND last_pos>=?";
        try (PreparedStatement sql = connection.prepareStatement("SELECT first_pos,last_pos FROM checkpoint_batch" + contact)) {
            sql.setInt(1, zoom);
            sql.setLong(2, to);
            sql.setLong(3, from);
            try (ResultSet rows = sql.executeQuery()) {
                while (rows.next()) {
                    long oldFirst = rows.getLong(1), oldLast = rows.getLong(2);
                    if (oldFirst < to && oldLast > from) throw new SQLException("预缓存检查点包含重复完成区间");
                    first = Math.min(first, oldFirst);
                    last = Math.max(last, oldLast);
                }
            }
        }
        try (PreparedStatement sql = connection.prepareStatement("DELETE FROM checkpoint_batch" + contact)) {
            sql.setInt(1, zoom);
            sql.setLong(2, to);
            sql.setLong(3, from);
            sql.executeUpdate();
        }
        try (PreparedStatement sql = connection.prepareStatement("INSERT INTO checkpoint_batch VALUES(?,?,?)")) {
            sql.setInt(1, zoom);
            sql.setLong(2, first);
            sql.setLong(3, last);
            sql.executeUpdate();
        }
    }

    private void addCounts(int zoom, int bucket, Map<? extends Enum<?>, Long> counts) throws SQLException {
        try (PreparedStatement sql = connection.prepareStatement("INSERT INTO checkpoint_count VALUES(?,?,?,?) "
                + "ON CONFLICT(z,bucket,status) DO UPDATE SET n=n+excluded.n")) {
            for (Map.Entry<? extends Enum<?>, Long> entry : counts.entrySet()) {
                if (entry.getValue() == 0) continue;
                sql.setInt(1, zoom);
                sql.setInt(2, bucket);
                sql.setString(3, entry.getKey().name());
                sql.setLong(4, entry.getValue());
                sql.addBatch();
            }
            sql.executeBatch();
        }
    }

    synchronized TilePrecacheProgress progress(int z, long total) throws SQLException {
        EnumMap<TileOperationStatus, Long> counts = new EnumMap<>(TileOperationStatus.class);
        for (TileOperationStatus status : TileOperationStatus.values()) counts.put(status, 0L);
        return new TilePrecacheProgress(z, total, readCounts(z, 0, TileOperationStatus.class, counts),
                new TilePrecacheStages(readCounts(z, 1, TileOperationStatus.class, null),
                        readCounts(z, 2, TileImageProcessingState.class, null),
                        readCounts(z, 3, TileImageWriteState.class, null)));
    }

    private <E extends Enum<E>> Map<E, Long> readCounts(int z, int bucket, Class<E> type,
            Map<E, Long> result) throws SQLException {
        if (result == null) result = new EnumMap<>(type);
        try (PreparedStatement sql = connection.prepareStatement("SELECT status,n FROM checkpoint_count WHERE z=? AND bucket=?")) {
            sql.setInt(1, z);
            sql.setInt(2, bucket);
            try (ResultSet rows = sql.executeQuery()) {
                while (rows.next()) result.put(Enum.valueOf(type, rows.getString(1)), rows.getLong(2));
            }
        }
        return result;
    }

    @Override
    public synchronized void close() throws SQLException {
        if (taskLock != null) throw new IllegalStateException("请等待预缓存任务结束后关闭检查点");
        connection.close();
    }
}
