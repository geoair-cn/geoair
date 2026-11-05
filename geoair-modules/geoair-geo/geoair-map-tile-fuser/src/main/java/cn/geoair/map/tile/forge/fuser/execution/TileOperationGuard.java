package cn.geoair.map.tile.forge.fuser.execution;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 注册可中止的 I/O；退出时立即移除定时器，避免大量瓦片遗留截止时间任务。 */
public final class TileOperationGuard implements AutoCloseable {
    private static final ScheduledThreadPoolExecutor TIMER =
            new ScheduledThreadPoolExecutor(
                    1,
                    r -> {
                        Thread thread = new Thread(r, "tile-operation-deadline");
                        thread.setDaemon(true);
                        return thread;
                    });

    static {
        TIMER.setRemoveOnCancelPolicy(true);
    }

    private final TileCancellationToken token;
    private final Runnable abort;
    private final ScheduledFuture<?> timer;

    public TileOperationGuard(TileOperationContext context, Runnable abort) {
        this.token = context.getCancellation();
        this.abort = abort;
        token.register(abort);
        try {
            timer = TIMER.schedule(abort, context.remainingMillis(), TimeUnit.MILLISECONDS);
        } catch (RuntimeException e) {
            token.unregister(abort);
            throw e;
        }
    }

    @Override
    public void close() {
        timer.cancel(false);
        token.unregister(abort);
    }
}
