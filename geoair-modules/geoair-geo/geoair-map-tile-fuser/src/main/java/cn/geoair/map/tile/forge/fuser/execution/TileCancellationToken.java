package cn.geoair.map.tile.forge.fuser.execution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** 每次任务独立持有；取消回调只作用于本任务的在途请求。 */
public class TileCancellationToken {
    private boolean cancelled;
    private final Set<Runnable> actions = new HashSet<>();

    public synchronized boolean isCancelled() {
        return cancelled;
    }

    public void throwIfCancelled() {
        if (isCancelled() || Thread.currentThread().isInterrupted())
            throw new TileOperationException(TileOperationStatus.CANCELLED, "瓦片任务已取消");
    }

    public void cancel() {
        ArrayList<Runnable> pending;
        synchronized (this) {
            if (cancelled) return;
            cancelled = true;
            pending = new ArrayList<>(actions);
            actions.clear();
        }
        for (Runnable action : pending) {
            try {
                action.run();
            } catch (RuntimeException ignored) {
                /* 继续取消其他请求 */
            }
        }
    }

    public void register(Runnable action) {
        synchronized (this) {
            if (!cancelled) {
                actions.add(action);
                return;
            }
        }
        action.run();
    }

    public synchronized void unregister(Runnable action) {
        actions.remove(action);
    }
}
