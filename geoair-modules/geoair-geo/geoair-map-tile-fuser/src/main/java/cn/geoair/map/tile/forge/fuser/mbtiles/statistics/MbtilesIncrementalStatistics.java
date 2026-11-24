package cn.geoair.map.tile.forge.fuser.mbtiles.statistics;

import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesWriteCoordinator;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesWriteLease;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import java.nio.file.Path;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** 对受支持的本机缓存显式启用事务统计；统计表和触发器位于缓存库内。 */

public class MbtilesIncrementalStatistics {
    private final MbtilesStatisticsOptions options;
    public MbtilesIncrementalStatistics() { this(new MbtilesStatisticsOptions()); }
    public MbtilesIncrementalStatistics(MbtilesStatisticsOptions options) { this.options = options.copy(); }


    private final TransactionalMbtilesStatistics adapter = new TransactionalMbtilesStatistics();

    public boolean installed(Path source) throws SQLException {
        try (Connection connection = open(source, true)) { return adapter.isInstalled(connection); }
    }

    public TransactionalStatsSnapshot read(Path source) throws SQLException {
        try (Connection connection = open(source, true)) {
            if (!adapter.isInstalled(connection)) return null;
            return adapter.read(connection);
        }
    }

    public void uninstall(Path source) throws SQLException {
        try (MbtilesWriteLease ignored = MbtilesWriteCoordinator.statistics(source);
             Connection connection = open(source, false)) {
            if (adapter.isInstalled(connection)) adapter.uninstall(connection);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("移除增量统计已中断", e);
        } catch (IOException e) {
            throw new SQLException("无法定位 MBTiles 统计文件", e);
        }
    }
    public TransactionalStatsSnapshot recalculateBounds(Path source, int zoom) throws SQLException {
        try (MbtilesWriteLease lease = MbtilesWriteCoordinator.statistics(source);
             Connection connection = open(source, false)) {
            if (!adapter.isInstalled(connection)) throw new SQLException("增量索引不存在");
            adapter.recalculateBounds(connection, zoom, lease::hasWaitingWrites);
            return adapter.read(connection);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("行列范围统计已中断", e);
        } catch (IOException e) {
            throw new SQLException("无法定位 MBTiles 统计文件", e);
        }
    }

    public TransactionalStatsSnapshot scan(Path source, Consumer<TransactionalStatsSnapshot> progress)
            throws SQLException, InterruptedException {
        return scan(source, progress, true);
    }
    public TransactionalStatsSnapshot resume(Path source, Consumer<TransactionalStatsSnapshot> progress)
            throws SQLException, InterruptedException {
        return scan(source, progress, false);
    }
    private TransactionalStatsSnapshot scan(Path source, Consumer<TransactionalStatsSnapshot> progress, boolean allowInstall)
            throws SQLException, InterruptedException {
        try (Connection connection = open(source, false)) {
            TransactionalStatsSnapshot snapshot = null;
            while (snapshot == null || !snapshot.isComplete()) {
                TileOperationContext.checkCurrent();
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException("统计已中断");
                try (MbtilesWriteLease lease = MbtilesWriteCoordinator.statistics(source)) {
                    if (!adapter.isInstalled(connection)) {
                        if (!allowInstall) throw new SQLException("缓存索引不存在，不能自动恢复");
                        adapter.install(connection);
                    }
                    snapshot = adapter.scanBatch(connection, options.getBatchSize(), TimeUnit.MILLISECONDS.toNanos(options.getSliceMillis()), lease::hasWaitingWrites);
                } catch (IOException e) {
                    throw new SQLException("无法定位 MBTiles 统计文件", e);
                } catch (SQLException e) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException("统计已中断");
                    // 外部进程也可能写库。短暂锁冲突先退让，不持有应用协调锁等待 SQLite。
                    int code = e.getErrorCode() & 255;
                    if (code != 5 && code != 6) throw e;
                    Thread.sleep(options.getYieldMillis());
                    continue;
                }
                progress.accept(snapshot);
                if (!snapshot.isComplete()) Thread.sleep(options.getYieldMillis());
            }
            return snapshot;
        }
    }

    public boolean boundsIncomplete(TransactionalStatsSnapshot snapshot) {
        for (TransactionalStatsLevel level : snapshot.getLevels()) if (level.isBoundsDirty()) return true;
        return false;
    }

    private Connection open(Path source, boolean readOnly) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + source.toUri()
                + (readOnly ? "?mode=ro" : "?mode=rw") + "&busy_timeout=" + (readOnly ? 1000 : options.getWriteBusyTimeoutMillis()));
    }
}
