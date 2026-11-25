package cn.geoair.map.tile.forge.fuser.fuser;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 限制出图线程、队列和等待调用者数量，同键在途请求共享一次执行。 */
public final class TileRenderQueue<K, V> implements AutoCloseable {
    private final ThreadPoolExecutor workers;
    private final Semaphore waiters;
    private final ConcurrentMap<K, TileRenderFlight<V>> active = new ConcurrentHashMap<>();

    public TileRenderQueue(int threads, int queued, int maxWaiters, ThreadFactory factory) {
        if (threads <= 0 || queued <= 0 || maxWaiters <= 0)
            throw new IllegalArgumentException("出图线程、队列和等待数量必须大于零");
        workers =
                new ThreadPoolExecutor(
                        threads,
                        threads,
                        0L,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(queued),
                        Objects.requireNonNull(factory, "线程工厂不能为空"));
        waiters = new Semaphore(maxWaiters);
    }

    /** 调用方将图层、发布版本、缓存代次和瓦片坐标编码到键中。取消某个订阅不会取消共享执行。 */
    public CompletableFuture<V> submit(K key, Callable<V> render) {
        Objects.requireNonNull(key, "出图键不能为空");
        Objects.requireNonNull(render, "出图操作不能为空");
        CompletableFuture<V> response = new CompletableFuture<>();
        if (workers.isShutdown() || !waiters.tryAcquire()) {
            response.completeExceptionally(new RejectedExecutionException("出图资源暂不可用"));
            return response;
        }
        TileRenderFlight<V> candidate = new TileRenderFlight<>();
        TileRenderFlight<V> existing = active.putIfAbsent(key, candidate);
        TileRenderFlight<V> shared = existing == null ? candidate : existing;
        response.whenComplete(
                (value, error) -> {
                    shared.unsubscribe(response);
                    waiters.release();
                });
        shared.subscribe(response);
        if (existing == null) {
            try {
                workers.execute(() -> render(key, render, candidate));
            } catch (RuntimeException rejected) {
                active.remove(key, candidate);
                candidate.finish(null, rejected);
            }
        }
        return response;
    }

    private void render(K key, Callable<V> render, TileRenderFlight<V> result) {
        V value = null;
        Throwable error = null;
        try {
            value = render.call();
        } catch (Throwable failure) {
            error = failure;
        }
        // 先释放键再通知调用方，回调中的显式重试才能发起新的出图。
        active.remove(key, result);
        result.finish(value, error);
        if (error instanceof Error) throw (Error) error;
    }

    @Override
    public void close() {
        workers.shutdownNow();
        active.forEach(
                (key, result) -> {
                    if (active.remove(key, result))
                        result.finish(null, new CancellationException("出图队列已关闭"));
                });
    }
}
