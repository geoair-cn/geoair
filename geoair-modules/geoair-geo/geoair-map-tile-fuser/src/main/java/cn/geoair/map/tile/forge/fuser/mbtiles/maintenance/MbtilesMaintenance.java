package cn.geoair.map.tile.forge.fuser.mbtiles.maintenance;

import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesAccessCoordinator;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesAccessLease;
import cn.hutool.core.bean.BeanUtil;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** 只操作显式指定的现有文件，不调度任务、不扫描部署目录、不改变文件名。 */
public class MbtilesMaintenance {
    private static final int INCREMENTAL_PAGES = 512;
    private final MbtilesMaintenanceDao dao;

    public MbtilesMaintenance() {
        this(new MbtilesMaintenanceDao());
    }

    public MbtilesMaintenance(MbtilesMaintenanceDao dao) {
        if (dao == null) throw new IllegalArgumentException("维护DAO不能为空");
        this.dao = dao;
    }

    public MbtilesFileInfo inspect(Path file) throws Exception {
        return dao.inspect(file);
    }

    public MbtilesMaintenanceResult checkpoint(
            Path file, MbtilesCheckpointMode mode, BooleanSupplier cancelled) throws Exception {
        if (mode == null || cancelled == null) throw new IllegalArgumentException("日志方式及取消信号不能为空");
        try (MbtilesAccessLease lease =
                mode == MbtilesCheckpointMode.PASSIVE
                        ? MbtilesAccessCoordinator.operation(file)
                        : MbtilesAccessCoordinator.maintenance(file, cancelled)) {
            check(cancelled);
            MbtilesFileInfo before = dao.inspect(file);
            MbtilesCheckpointResult checkpoint = checkpointResult(file, mode, cancelled);
            return result(before, dao.inspect(file), checkpoint);
        } finally {
            TileOperationContext.checkCurrent();
        }
    }

    public MbtilesMaintenanceResult reclaim(
            Path file,
            MbtilesReclaimMode mode,
            BooleanSupplier cancelled,
            Consumer<MbtilesFileInfo> progress)
            throws Exception {
        if (mode == null || cancelled == null) throw new IllegalArgumentException("回收方式及取消信号不能为空");
        boolean incremental;
        try (MbtilesAccessLease task = MbtilesAccessCoordinator.task(file, cancelled)) {
            check(cancelled);
            incremental =
                    mode == MbtilesReclaimMode.INCREMENTAL
                            || mode == MbtilesReclaimMode.AUTO
                                    && dao.inspect(file).isIncrementalSupported();
            if (incremental) return incremental(file, cancelled, progress);
        }
        try (MbtilesAccessLease lease = MbtilesAccessCoordinator.maintenance(file, cancelled)) {
            check(cancelled);
            MbtilesFileInfo before = dao.inspect(file);
            if (mode == MbtilesReclaimMode.AUTO && before.getFreePages() == 0)
                return result(before, before, null);
            if (!before.isVacuumSpaceEnough()) throw new IllegalStateException("完整整理可用磁盘空间不足");
            MbtilesCheckpointResult checkpoint =
                    checkpointResult(file, MbtilesCheckpointMode.TRUNCATE, cancelled);
            if (!checkpoint.isComplete()) return result(before, dao.inspect(file), checkpoint);
            check(cancelled);
            dao.vacuum(file, cancelled);
            check(cancelled);
            checkpoint = checkpointResult(file, MbtilesCheckpointMode.TRUNCATE, cancelled);
            MbtilesFileInfo after = dao.inspect(file);
            publish(progress, after);
            return result(before, after, checkpoint);
        } finally {
            TileOperationContext.checkCurrent();
        }
    }

    private MbtilesMaintenanceResult incremental(
            Path file, BooleanSupplier cancelled, Consumer<MbtilesFileInfo> progress)
            throws Exception {
        MbtilesFileInfo before = dao.inspect(file), current = before;
        if (!before.isIncrementalSupported())
            throw new IllegalArgumentException("该库未启用增量回收，需要显式完整整理");
        while (current.getFreePages() > 0) {
            check(cancelled);
            long reclaimed = dao.incremental(file, INCREMENTAL_PAGES, cancelled);
            current = dao.inspect(file);
            publish(progress, current);
            if (reclaimed == 0 && current.getFreePages() > 0)
                throw new IllegalStateException("本批没有可回收进展，请稍后重试");
            if (current.getFreePages() > 0) Thread.sleep(10);
        }
        MbtilesCheckpointResult checkpoint =
                checkpointResult(file, MbtilesCheckpointMode.PASSIVE, cancelled);
        return result(before, dao.inspect(file), checkpoint);
    }

    private MbtilesCheckpointResult checkpointResult(
            Path file, MbtilesCheckpointMode mode, BooleanSupplier cancelled) throws Exception {
        check(cancelled);
        int[] values = dao.checkpoint(file, mode, cancelled);
        return new MbtilesCheckpointResult(values[0], values[1], values[2]);
    }

    private MbtilesMaintenanceResult result(
            MbtilesFileInfo before, MbtilesFileInfo after, MbtilesCheckpointResult checkpoint) {
        return new MbtilesMaintenanceResult(
                checkpoint == null || checkpoint.isComplete()
                        ? MbtilesMaintenanceStatus.COMPLETED
                        : MbtilesMaintenanceStatus.PARTIAL,
                before,
                after,
                checkpoint);
    }

    private void publish(Consumer<MbtilesFileInfo> listener, MbtilesFileInfo info) {
        if (listener != null) listener.accept(BeanUtil.copyProperties(info, MbtilesFileInfo.class));
    }

    private void check(BooleanSupplier cancelled) throws InterruptedException {
        TileOperationContext.checkCurrent();
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted())
            throw new InterruptedException("维护已取消");
    }
}
