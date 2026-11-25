package cn.geoair.map.tile.forge.fuser.mbtiles;

import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** 同一进程按物理文件协调写入，等待中的瓦片写入优先于统计。 */
public final class MbtilesWriteCoordinator {
    private static final Map<Path, MbtilesWriteCoordinator> FILES = new HashMap<>();
    private final Path path;
    private final AtomicInteger waitingWrites = new AtomicInteger();
    private Thread owner;
    private int depth;
    private int users;

    private MbtilesWriteCoordinator(Path path) {
        this.path = path;
    }

    public static MbtilesWriteLease write(Path path) throws IOException, InterruptedException {
        return acquire(path, true);
    }

    public static MbtilesWriteLease statistics(Path path) throws IOException, InterruptedException {
        return acquire(path, false);
    }

    private static MbtilesWriteLease acquire(Path path, boolean writer)
            throws IOException, InterruptedException {
        MbtilesAccessLease access =
                writer
                        ? MbtilesAccessCoordinator.operation(path)
                        : MbtilesAccessCoordinator.backgroundRead(path);
        MbtilesWriteCoordinator gate;
        try {
            gate = reserve(path);
        } catch (IOException | RuntimeException failure) {
            access.close();
            throw failure;
        }
        try {
            MbtilesWriteLease lease = new MbtilesWriteLease(gate, access);
            synchronized (gate) {
                if (writer) gate.waitingWrites.incrementAndGet();
                try {
                    Thread current = Thread.currentThread();
                    while (gate.owner != current
                            && (gate.owner != null || (!writer && gate.waitingWrites.get() > 0))) {
                        TileOperationContext.checkCurrent();
                        gate.wait(50);
                    }
                    if (current.isInterrupted()) throw new InterruptedException("MBTiles 写入等待已中断");
                    gate.owner = current;
                    gate.depth++;
                    return lease;
                } finally {
                    if (writer) gate.waitingWrites.decrementAndGet();
                    // 等待中的写入被取消后，也要唤醒低优先级统计，避免永久挂起。
                    gate.notifyAll();
                }
            }
        } catch (InterruptedException | RuntimeException | Error e) {
            forget(gate);
            access.close();
            throw e;
        }
    }

    private static synchronized MbtilesWriteCoordinator reserve(Path file) throws IOException {
        Path absolute = file.toAbsolutePath().normalize();
        Path identity =
                Files.exists(absolute)
                        ? absolute.toRealPath()
                        : absolute.getParent().toRealPath().resolve(absolute.getFileName());
        MbtilesWriteCoordinator gate =
                FILES.computeIfAbsent(identity, MbtilesWriteCoordinator::new);
        gate.users++;
        return gate;
    }

    private static synchronized void forget(MbtilesWriteCoordinator gate) {
        if (--gate.users == 0) FILES.remove(gate.path, gate);
    }

    boolean hasWaitingWrites() {
        return waitingWrites.get() > 0;
    }

    void release() {
        synchronized (this) {
            if (owner != Thread.currentThread())
                throw new IllegalStateException("MBTiles 使用权必须由获取线程归还");
            if (--depth == 0) owner = null;
            notifyAll();
        }
        forget(this);
    }
}
