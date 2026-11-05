package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.cache.TileCacheQuery;
import cn.geoair.map.tile.forge.fuser.execution.TileCancellationToken;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationException;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;
import cn.geoair.web.mime.GiMimeType;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** 供业务层复用的同步入口。协调线程不能占用传入的工作池；线程池生命周期归调用方。 */
public final class SegmentedTilePrecache {
    private SegmentedTilePrecache() {}

    public static List<TilePrecacheProgress> execute(
            String layer,
            List<TilePrecacheLevel> levels,
            GiMimeType format,
            TileCache cache,
            TileContentLoader loader,
            ExecutorService workers,
            TileCancellationToken cancellation,
            TilePrecacheOptions options,
            Consumer<TilePrecacheProgress> listener) {
        Objects.requireNonNull(levels);
        Objects.requireNonNull(cache);
        Objects.requireNonNull(loader);
        Objects.requireNonNull(workers);
        Objects.requireNonNull(cancellation);
        Objects.requireNonNull(options);
        options = options.copy();
        options.validate();
        if (!cache.isEnabled()) throw new IllegalArgumentException("预缓存需要启用缓存驱动");
        List<TilePrecacheLevel> plans = new ArrayList<>(levels);
        List<TilePrecacheCounter> counters = new ArrayList<>();
        Set<Integer> zooms = new HashSet<>();
        for (TilePrecacheLevel level : plans) {
            if (!zooms.add(level.getZoom())) throw new IllegalArgumentException("同一批次的级别不能重复");
            new TileCacheQuery(layer, level.getZoom(), level.getRange(), 0, 0, format);
            TilePrecacheCounter counter = new TilePrecacheCounter(level, listener);
            counters.add(counter);
            counter.publish();
        }
        long[] next = new long[plans.size()];
        CompletionService<Void> completion = new ExecutorCompletionService<>(workers);
        int pending = 0, cursor = 0;
        Throwable failure = null;
        boolean interrupted = false;
        try {
            // 轮流投递不同级别的分段，避免低级别占满队列。窗口只限制排队内存。
            while (true) {
                boolean submitted = false;
                if (!cancellation.isCancelled()) {
                    for (int visited = 0;
                            visited < plans.size() && pending < options.getPendingSegments();
                            visited++) {
                        int i = cursor;
                        cursor = (cursor + 1) % plans.size();
                        TilePrecacheLevel level = plans.get(i);
                        if (next[i] >= level.getTotal()) continue;
                        long end =
                                next[i]
                                        + Math.min(
                                                options.getSegmentSize(),
                                                level.getTotal() - next[i]);
                        completion.submit(
                                new TilePrecacheSegment(
                                        level,
                                        new TileCacheQuery(
                                                layer,
                                                level.getZoom(),
                                                level.getRange(),
                                                next[i],
                                                end,
                                                format),
                                        counters.get(i),
                                        cache,
                                        loader,
                                        cancellation,
                                        options));
                        pending++;
                        next[i] = end;
                        submitted = true;
                    }
                }
                if (submitted && pending < options.getPendingSegments()) continue;
                if (pending == 0) break;
                Future<Void> finished = completion.take();
                pending--;
                finished.get();
            }
        } catch (InterruptedException e) {
            interrupted = true;
            failure = e;
            cancellation.cancel();
        } catch (Throwable e) {
            failure = e instanceof ExecutionException ? e.getCause() : e;
            cancellation.cancel();
        } finally {
            // 不用 Future.cancel 冒充结束；必须等已投递工作真正退出再向业务层报告结束。
            while (pending > 0) {
                try {
                    Future<Void> finished = completion.take();
                    pending--;
                    finished.get();
                } catch (InterruptedException e) {
                    interrupted = true;
                    cancellation.cancel();
                } catch (ExecutionException e) {
                    if (failure == null) failure = e.getCause();
                }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
        if (failure instanceof Error) throw (Error) failure;
        if (failure != null
                && !(failure instanceof InterruptedException)
                && !(failure instanceof TileOperationException
                        && ((TileOperationException) failure).getStatus()
                                == TileOperationStatus.CANCELLED))
            throw new IllegalStateException("预缓存分段执行失败", failure);
        List<TilePrecacheProgress> result = new ArrayList<>();
        for (TilePrecacheCounter counter : counters) result.add(counter.snapshot());
        return result;
    }
}
