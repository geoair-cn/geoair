package cn.geoair.map.tile.forge.fuser.mbtiles;

import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** 多库任务一次取得全部登记，失败先释放再等待，避免多个维护请求交叉等待。 */
public final class MbtilesTaskReservation implements AutoCloseable {
    private final List<MbtilesAccessLease> leases;

    private MbtilesTaskReservation(List<MbtilesAccessLease> leases) {
        this.leases = leases;
    }

    public static MbtilesTaskReservation acquire(List<Path> paths, BooleanSupplier cancelled)
            throws Exception {
        if (paths == null || cancelled == null) throw new IllegalArgumentException("物理文件和取消信号不能为空");
        while (true) {
            TileOperationContext.checkCurrent();
            if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted())
                throw new InterruptedException("任务已停止");
            List<MbtilesAccessLease> acquired = new ArrayList<>();
            boolean complete = false;
            try {
                for (Path path : paths) {
                    MbtilesAccessLease lease = MbtilesAccessCoordinator.tryTask(path);
                    if (lease == null) break;
                    acquired.add(lease);
                }
                if (acquired.size() == paths.size()) {
                    complete = true;
                    return new MbtilesTaskReservation(acquired);
                }
            } finally {
                if (!complete) for (MbtilesAccessLease lease : acquired) lease.close();
            }
            Thread.sleep(100);
        }
    }

    @Override
    public void close() {
        for (MbtilesAccessLease lease : leases) lease.close();
    }
}
