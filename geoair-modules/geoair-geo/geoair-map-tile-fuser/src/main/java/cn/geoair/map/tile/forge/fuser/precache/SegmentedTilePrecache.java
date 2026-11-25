package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.cache.TileCacheQuery;
import cn.geoair.map.tile.forge.fuser.encoding.TileEncodingContext;
import cn.geoair.map.tile.forge.fuser.execution.TileCancellationToken;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationException;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;
import cn.geoair.web.mime.GiMimeType;
import cn.hutool.crypto.SecureUtil;

import java.sql.SQLException;
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
        return execute(
                layer,
                levels,
                format,
                cache,
                loader,
                null,
                workers,
                cancellation,
                options,
                listener);
    }

    /** 显式的预缓存与处理组合入口；旧签名继续只下载缺片。 */
    public static List<TilePrecacheProgress> execute(
            String layer,
            List<TilePrecacheLevel> levels,
            GiMimeType format,
            TileCache cache,
            TileContentLoader loader,
            TilePrecachePipeline pipeline,
            ExecutorService workers,
            TileCancellationToken cancellation,
            TilePrecacheOptions options,
            Consumer<TilePrecacheProgress> listener) {
        return execute(
                layer,
                levels,
                format,
                cache,
                loader,
                pipeline,
                workers,
                cancellation,
                options,
                listener,
                null,
                null,
                null,
                false);
    }

    /** 库负责范围和格式校验；调用方提供图层版本、缓存身份、矩阵/行原点及处理策略的稳定指纹。 */
    public static List<TilePrecacheProgress> executeResumable(
            String layer,
            List<TilePrecacheLevel> levels,
            GiMimeType format,
            TileCache cache,
            TileContentLoader loader,
            TilePrecachePipeline pipeline,
            ExecutorService workers,
            TileCancellationToken cancellation,
            TilePrecacheOptions options,
            Consumer<TilePrecacheProgress> listener,
            TilePrecacheBatchGate gate,
            String taskIdentity,
            TilePrecacheCheckpoint checkpoint,
            boolean retryFailuresOnly) {
        Objects.requireNonNull(gate, "批次调度器不能为空");
        if (taskIdentity == null || taskIdentity.trim().isEmpty())
            throw new IllegalArgumentException("预缓存任务身份不能为空");
        if (retryFailuresOnly && checkpoint == null)
            throw new IllegalArgumentException("补跑失败瓦片需要有效检查点");
        return execute(
                layer,
                levels,
                format,
                cache,
                loader,
                pipeline,
                workers,
                cancellation,
                options,
                listener,
                gate,
                taskIdentity,
                checkpoint,
                retryFailuresOnly);
    }

    private static List<TilePrecacheProgress> execute(
            String layer,
            List<TilePrecacheLevel> levels,
            GiMimeType format,
            TileCache cache,
            TileContentLoader loader,
            TilePrecachePipeline pipeline,
            ExecutorService workers,
            TileCancellationToken cancellation,
            TilePrecacheOptions suppliedOptions,
            Consumer<TilePrecacheProgress> listener,
            TilePrecacheBatchGate gate,
            String taskIdentity,
            TilePrecacheCheckpoint checkpoint,
            boolean retryFailuresOnly) {
        Objects.requireNonNull(levels);
        Objects.requireNonNull(cache);
        Objects.requireNonNull(loader);
        Objects.requireNonNull(workers);
        Objects.requireNonNull(cancellation);
        TilePrecacheOptions options = suppliedOptions.copy();
        options.validate();
        if (!cache.isEnabled()) throw new IllegalArgumentException("预缓存需要启用缓存驱动");
        if (pipeline != null) pipeline.validateSource(layer);
        // 协调线程只捕获一次快照，随后显式传给各工作线程，不依赖线程池继承上下文。
        TileEncodingContext encoding = TileEncodingContext.capture();
        List<TilePrecacheLevel> plans = new ArrayList<>(levels);
        List<TilePrecacheCounter> counters = new ArrayList<>();
        Set<Integer> zooms = new HashSet<>();
        for (TilePrecacheLevel level : plans) {
            if (!zooms.add(level.getZoom())) throw new IllegalArgumentException("同一批次的级别不能重复");
            new TileCacheQuery(layer, level.getZoom(), level.getRange(), 0, 0, format);
            TilePrecacheCounter counter = new TilePrecacheCounter(level, listener);
            counters.add(counter);
            if (checkpoint == null) counter.publish();
        }
        int batchSize =
                gate == null
                        ? options.getSegmentSize()
                        : Math.min(options.getSegmentSize(), options.getBatchSize());
        if (checkpoint != null) {
            try {
                checkpoint.begin(identity(layer, format, plans, taskIdentity), retryFailuresOnly);
            } catch (SQLException failure) {
                throw new IllegalStateException("无法打开预缓存检查点", failure);
            }
        }
        long[] next = new long[plans.size()];
        CompletionService<Void> completion = new ExecutorCompletionService<>(workers);
        int pending = 0, cursor = 0;
        Throwable failure = null;
        boolean interrupted = false;
        try {
            if (checkpoint != null && listener != null) {
                for (TilePrecacheLevel level : plans)
                    listener.accept(checkpoint.progress(level.getZoom(), level.getTotal()));
            }
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
                        long start = next[i];
                        if (checkpoint != null && !retryFailuresOnly) {
                            long completed;
                            while (start < level.getTotal()
                                    && (completed = checkpoint.completedTo(level.getZoom(), start))
                                            > start) start = completed;
                            next[i] = start;
                        }
                        if (start >= level.getTotal()) continue;
                        long end = start + Math.min(batchSize, level.getTotal() - start);
                        if (checkpoint != null && !retryFailuresOnly)
                            end = checkpoint.gapEnd(level.getZoom(), start, end);
                        List<TilePrecacheFailure> previous = Collections.emptyList();
                        BitSet selection = null;
                        if (retryFailuresOnly) {
                            List<TilePrecacheFailure> page =
                                    checkpoint.failures(level.getZoom(), start - 1, batchSize);
                            if (page.isEmpty()) {
                                next[i] = level.getTotal();
                                continue;
                            }
                            start = page.get(0).getPosition();
                            long limit = start + Math.min(batchSize, level.getTotal() - start);
                            int count = 0;
                            while (count < page.size() && page.get(count).getPosition() < limit)
                                count++;
                            previous = page.subList(0, count);
                            end = previous.get(count - 1).getPosition() + 1;
                            selection = new BitSet((int) (end - start));
                            for (TilePrecacheFailure failed : previous)
                                selection.set((int) (failed.getPosition() - start));
                        }
                        TileCacheQuery query =
                                new TileCacheQuery(
                                        layer,
                                        level.getZoom(),
                                        level.getRange(),
                                        start,
                                        end,
                                        format);
                        TilePrecacheCounter counter =
                                checkpoint == null
                                        ? counters.get(i)
                                        : new TilePrecacheCounter(level, null);
                        List<TilePrecacheFailure> failures =
                                checkpoint == null ? null : new ArrayList<>();
                        TilePrecacheSegment segment =
                                new TilePrecacheSegment(
                                        level,
                                        query,
                                        counter,
                                        cache,
                                        loader,
                                        cancellation,
                                        options,
                                        pipeline,
                                        encoding,
                                        selection,
                                        failures);
                        List<TilePrecacheFailure> replay = previous;
                        if (gate != null) gate.acquire(taskIdentity, cancellation);
                        try {
                            completion.submit(
                                    () -> {
                                        try {
                                            segment.call();
                                            if (checkpoint != null) {
                                                // 只有已处理完整批次才提交；停止或崩溃后，未提交批次可以安全复查缓存。
                                                synchronized (checkpoint) {
                                                    checkpoint.commit(
                                                            level.getZoom(),
                                                            query.getFrom(),
                                                            query.getTo(),
                                                            counter.snapshot(),
                                                            failures,
                                                            replay);
                                                    if (listener != null)
                                                        listener.accept(
                                                                checkpoint.progress(
                                                                        level.getZoom(),
                                                                        level.getTotal()));
                                                }
                                            }
                                            return null;
                                        } finally {
                                            if (gate != null) gate.release();
                                        }
                                    });
                        } catch (RuntimeException | Error rejected) {
                            if (gate != null) gate.release();
                            throw rejected;
                        }
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
        try {
            if (failure instanceof Error) throw (Error) failure;
            if (failure != null
                    && !(failure instanceof InterruptedException)
                    && !(failure instanceof TileOperationException
                            && ((TileOperationException) failure).getStatus()
                                    == TileOperationStatus.CANCELLED))
                throw new IllegalStateException("预缓存分段执行失败", failure);
            List<TilePrecacheProgress> result = new ArrayList<>();
            for (int i = 0; i < plans.size(); i++) {
                TilePrecacheLevel level = plans.get(i);
                result.add(
                        checkpoint == null
                                ? counters.get(i).snapshot()
                                : checkpoint.progress(level.getZoom(), level.getTotal()));
            }
            return result;
        } catch (SQLException problem) {
            throw new IllegalStateException("读取断点进度失败", problem);
        } finally {
            if (checkpoint != null) checkpoint.end();
        }
    }

    private static String identity(
            String layer, GiMimeType format, List<TilePrecacheLevel> plans, String taskIdentity) {
        StringBuilder text =
                new StringBuilder("tile-precache-v1|")
                        .append(taskIdentity.length())
                        .append(':')
                        .append(taskIdentity)
                        .append('|')
                        .append(layer.length())
                        .append(':')
                        .append(layer)
                        .append('|')
                        .append(format.getFormat());
        List<TilePrecacheLevel> ordered = new ArrayList<>(plans);
        ordered.sort(Comparator.comparingInt(TilePrecacheLevel::getZoom));
        for (TilePrecacheLevel level : ordered) {
            RangeApo range = level.getRange();
            text.append('|')
                    .append(level.getZoom())
                    .append(':')
                    .append(range.getMinX())
                    .append(':')
                    .append(range.getMaxX())
                    .append(':')
                    .append(range.getMinY())
                    .append(':')
                    .append(range.getMaxY());
            if (level.getMask() != null)
                text.append(':').append(SecureUtil.sha256(level.getMask().checkpointGeometry()));
        }
        return SecureUtil.sha256(text.toString());
    }
}
