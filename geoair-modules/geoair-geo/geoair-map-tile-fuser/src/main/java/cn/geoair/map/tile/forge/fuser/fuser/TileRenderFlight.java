package cn.geoair.map.tile.forge.fuser.fuser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** 主动移除取消等待的调用者，避免慢出图挂住反复取消的回调。 */
final class TileRenderFlight<V> {
    private final Set<CompletableFuture<V>> subscribers = new HashSet<>();
    private boolean finished;
    private V value;
    private Throwable error;

    void subscribe(CompletableFuture<V> subscriber) {
        synchronized (this) {
            if (!finished) {
                subscribers.add(subscriber);
                return;
            }
        }
        complete(subscriber);
    }

    synchronized void unsubscribe(CompletableFuture<V> subscriber) {
        subscribers.remove(subscriber);
    }

    void finish(V result, Throwable failure) {
        List<CompletableFuture<V>> pending;
        synchronized (this) {
            if (finished) return;
            value = result;
            error = failure;
            finished = true;
            pending = new ArrayList<>(subscribers);
            subscribers.clear();
        }
        // 执行调用方回调时不持有共享锁，其他订阅可以独立注册或取消。
        pending.forEach(this::complete);
    }

    private void complete(CompletableFuture<V> subscriber) {
        if (error == null) subscriber.complete(value);
        else subscriber.completeExceptionally(error);
    }
}
