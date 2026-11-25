package cn.geoair.map.tile.forge.fuser.mbtiles;

import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/** 按真实物理路径协调维护；任务先登记，已有任务可以收尾，新任务在维护后开始。 */
public final class MbtilesAccessCoordinator {
    private static final Map<Path, MbtilesAccessState> FILES = new HashMap<>();
    private static final long WAIT_MILLIS = 100;

    private MbtilesAccessCoordinator() {}

    private static synchronized MbtilesAccessState reserve(Path file) throws IOException {
        Path absolute = file.toAbsolutePath().normalize();
        Path identity =
                Files.exists(absolute)
                        ? absolute.toRealPath()
                        : absolute.getParent().toRealPath().resolve(absolute.getFileName());
        MbtilesAccessState state = FILES.computeIfAbsent(identity, MbtilesAccessState::new);
        state.reservations++;
        return state;
    }

    private static synchronized void forget(MbtilesAccessState state) {
        if (--state.reservations == 0) FILES.remove(state.path, state);
    }

    public static MbtilesAccessLease operation(Path file) throws IOException, InterruptedException {
        return acquire(
                file, MbtilesAccessKind.OPERATION, () -> Thread.currentThread().isInterrupted());
    }

    /** 后台只读统计按批等待；不占任务名额，维护可以在两批之间进入。 */
    public static MbtilesAccessLease backgroundRead(Path file) throws IOException {
        MbtilesAccessState state = reserve(file);
        try {
            synchronized (state) {
                while (state.maintenanceOwner != Thread.currentThread()
                        && (state.maintenanceOwner != null || state.pending && state.tasks == 0))
                    await(state, () -> Thread.currentThread().isInterrupted());
                check(() -> Thread.currentThread().isInterrupted());
                state.operations++;
                return new MbtilesAccessLease(state, MbtilesAccessKind.OPERATION);
            }
        } catch (InterruptedException stopped) {
            forget(state);
            Thread.currentThread().interrupt();
            throw new IOException("统计等待维护时已停止", stopped);
        } catch (RuntimeException | Error failure) {
            forget(state);
            throw failure;
        }
    }

    public static MbtilesAccessLease task(Path file, BooleanSupplier cancelled)
            throws IOException, InterruptedException {
        return acquire(file, MbtilesAccessKind.TASK, cancelled);
    }

    static MbtilesAccessLease tryTask(Path file) throws IOException {
        MbtilesAccessState state = reserve(file);
        synchronized (state) {
            if (!state.pending && state.maintenanceOwner == null) {
                state.tasks++;
                return new MbtilesAccessLease(state, MbtilesAccessKind.TASK);
            }
        }
        forget(state);
        return null;
    }

    public static void requireMaintenance(Path file) throws IOException {
        MbtilesAccessState state = reserve(file);
        try {
            synchronized (state) {
                if (state.maintenanceOwner != Thread.currentThread())
                    throw new IllegalStateException("完整整理必须在独占维护范围内执行");
            }
        } finally {
            forget(state);
        }
    }

    public static MbtilesAccessLease maintenance(Path file, BooleanSupplier cancelled)
            throws IOException, InterruptedException {
        return acquire(file, MbtilesAccessKind.MAINTENANCE, cancelled);
    }

    private static MbtilesAccessLease acquire(
            Path file, MbtilesAccessKind kind, BooleanSupplier cancelled)
            throws IOException, InterruptedException {
        MbtilesAccessState state = reserve(file);
        boolean claimedPending = false;
        try {
            synchronized (state) {
                if (kind == MbtilesAccessKind.MAINTENANCE) {
                    while (state.pending || state.maintenanceOwner != null) await(state, cancelled);
                    state.pending = true;
                    claimedPending = true;
                    // 等待旧任务完成，期间允许其工作线程继续读写，禁止新后台任务插队。
                    while (state.tasks > 0 || state.operations > 0) await(state, cancelled);
                    check(cancelled);
                    state.maintenanceOwner = Thread.currentThread();
                } else if (kind == MbtilesAccessKind.TASK) {
                    while (state.pending || state.maintenanceOwner != null) await(state, cancelled);
                    check(cancelled);
                    state.tasks++;
                } else {
                    if (state.maintenanceOwner != Thread.currentThread()
                            && (state.maintenanceOwner != null
                                    || state.pending && state.tasks == 0))
                        throw new MbtilesMaintenanceException();
                    check(cancelled);
                    state.operations++;
                }
                return new MbtilesAccessLease(state, kind);
            }
        } catch (InterruptedException | RuntimeException | Error failure) {
            if (claimedPending)
                synchronized (state) {
                    state.pending = false;
                    state.notifyAll();
                }
            forget(state);
            throw failure;
        }
    }

    private static void check(BooleanSupplier cancelled) throws InterruptedException {
        TileOperationContext.checkCurrent();
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted())
            throw new InterruptedException("缓存操作已停止");
    }

    private static void await(MbtilesAccessState state, BooleanSupplier cancelled)
            throws InterruptedException {
        check(cancelled);
        state.wait(WAIT_MILLIS);
    }

    static void release(MbtilesAccessState state, MbtilesAccessKind kind) {
        synchronized (state) {
            if (kind == MbtilesAccessKind.MAINTENANCE
                    && state.maintenanceOwner != Thread.currentThread())
                throw new IllegalStateException("独占维护必须由执行线程收尾");
            if (kind == MbtilesAccessKind.OPERATION) state.operations--;
            else if (kind == MbtilesAccessKind.TASK) state.tasks--;
            else {
                state.maintenanceOwner = null;
                state.pending = false;
            }
            state.notifyAll();
        }
        forget(state);
    }
}
