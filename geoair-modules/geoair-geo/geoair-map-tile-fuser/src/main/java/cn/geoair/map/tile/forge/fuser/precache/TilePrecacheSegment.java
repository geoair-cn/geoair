package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.cache.TileCacheQuery;
import cn.geoair.map.tile.forge.fuser.execution.TileCancellationToken;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationException;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationScope;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;
import cn.geoair.map.tile.forge.fuser.encoding.TileEncodingContext;
import cn.geoair.map.tile.forge.fuser.encoding.TileEncodingScope;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingOutcome;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingState;
import cn.geoair.map.tile.forge.fuser.utils.TileResourceLimits;
import java.io.IOException;

import java.util.BitSet;
import java.util.List;
import java.util.concurrent.Callable;

/** 一段内批量筛选范围和缓存，仅下载未命中的瓦片。 */
final class TilePrecacheSegment implements Callable<Void> {
    final TilePrecacheLevel level;
    final TileCacheQuery query;
    final TilePrecacheCounter counter;
    final TileCache cache;
    final TileContentLoader loader;
    final TileCancellationToken cancellation;
    final TilePrecacheOptions options;
    final TilePrecachePipeline pipeline;
    final TileEncodingContext encoding;
    private final BitSet selection;
    private final List<TilePrecacheFailure> failures;

    TilePrecacheSegment(
            TilePrecacheLevel level, TileCacheQuery query, TilePrecacheCounter counter, TileCache cache,
            TileContentLoader loader, TileCancellationToken cancellation, TilePrecacheOptions options,
            TilePrecachePipeline pipeline, TileEncodingContext encoding) {
        this(level, query, counter, cache, loader, cancellation, options, pipeline, encoding, null, null);
    }

    TilePrecacheSegment(
            TilePrecacheLevel level,
            TileCacheQuery query,
            TilePrecacheCounter counter,
            TileCache cache,
            TileContentLoader loader,
            TileCancellationToken cancellation,
            TilePrecacheOptions options, TilePrecachePipeline pipeline, TileEncodingContext encoding,
            BitSet selection, List<TilePrecacheFailure> failures) {
        this.level = level;
        this.query = query;
        this.counter = counter;
        this.cache = cache;
        this.loader = loader;
        this.cancellation = cancellation;
        this.options = options;
        this.pipeline = pipeline; this.encoding = encoding;
        this.selection = selection;
        this.failures = failures;
    }

    public Void call() {
        TileOperationContext inspection =
                new TileOperationContext(cancellation, options.getInspectionTimeoutMillis());
        try (TileEncodingScope taskEncoding = encoding.bind(); TileOperationScope ignored = inspection.bind()) {
            inspection.check();
            BitSet inside;
            if (level.getMask() == null) {
                inside = new BitSet(query.size());
                inside.set(0, query.size());
            } else
                inside =
                        level.getMask()
                                .classify(
                                        query.getRange(),
                                        query.getRows(),
                                        query.getFrom(),
                                        query.getTo());
            inspection.check();
            if (selection != null) inside.and(selection);
            int candidates = selection == null ? query.size() : selection.cardinality();
            counter.add(TileOperationStatus.SKIPPED, candidates - inside.cardinality());
            if (inside.isEmpty()) return null;
            BitSet existing = cache.findExisting(query, inspection);
            existing.and(inside);
            if (pipeline != null) {
                for (int offset = inside.nextSetBit(0); offset >= 0; offset = inside.nextSetBit(offset + 1)) {
                    cancellation.throwIfCancelled();
                    TilePrecacheTileResult result = processCombined(query.getFrom() + offset, existing.get(offset));
                    counter.add(result);
                    recordFailure(query.getFrom() + offset, result.getStatus());
                    // 回调故障是接入故障，不能吞成一张图片的处理失败。
                    pipeline.completed(result);
                    if (result.getStatus() == TileOperationStatus.CANCELLED)
                        throw new TileOperationException(TileOperationStatus.CANCELLED, "预缓存与处理已停止");
                }
                return null;
            }
            counter.add(TileOperationStatus.CACHED, existing.cardinality());
            inside.andNot(existing);
            for (int offset = inside.nextSetBit(0);
                    offset >= 0;
                    offset = inside.nextSetBit(offset + 1)) {
                inspection.getCancellation().throwIfCancelled();
                TileOperationContext tile =
                        new TileOperationContext(cancellation, options.getTileTimeoutMillis());
                TileOperationStatus status;
                try (TileOperationScope scope = tile.bind()) {
                    tile.check();
                    long index = query.getFrom() + offset;
                    byte[] data =
                            loader.load(query.getZoom(), query.x(index), query.y(index), tile);
                    tile.check();
                    if (data == null || data.length == 0) status = TileOperationStatus.MISSING;
                    else {
                        boolean saved =
                                cache.put(
                                        query.getLayerName(),
                                        query.getZoom(),
                                        query.x(index),
                                        query.y(index),
                                        data,
                                        query.getFormat());
                        tile.check();
                        status =
                                saved ? TileOperationStatus.DOWNLOADED : TileOperationStatus.FAILED;
                    }
                } catch (TileOperationException e) {
                    if (e.getStatus() == TileOperationStatus.CANCELLED) throw e;
                    status = e.getStatus();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    cancellation.cancel();
                    throw new TileOperationException(TileOperationStatus.CANCELLED, "预缓存已停止");
                } catch (Exception e) {
                    status = TileOperationStatus.FAILED;
                    try {
                        tile.check();
                    } catch (TileOperationException interrupted) {
                        if (interrupted.getStatus() == TileOperationStatus.CANCELLED)
                            throw interrupted;
                        status = interrupted.getStatus();
                    }
                }
                counter.add(status, 1);
                recordFailure(query.getFrom() + offset, status);
            }
            return null;
        } finally {
            counter.publish();
        }
    }

    private void recordFailure(long position, TileOperationStatus status) {
        if (failures != null && TilePrecacheFailure.retryable(status))
            failures.add(new TilePrecacheFailure(position, query.x(position), query.y(position), status));
    }

    private TilePrecacheTileResult processCombined(long index, boolean existed) {
        int zoom = query.getZoom(), x = query.x(index), y = query.y(index);
        TileOperationContext tile = new TileOperationContext(cancellation, options.getTileTimeoutMillis());
        TileOperationStatus source = null, status = TileOperationStatus.FAILED;
        TileImageProcessingOutcome processed = null;
        Throwable failure = null;
        try (TileOperationScope scope = tile.bind()) {
            tile.check();
            byte[] beforeEncoding = existed ? cache.get(query.getLayerName(), zoom, x, y, query.getFormat()) : null;
            tile.check();
            if (beforeEncoding != null && beforeEncoding.length > 0) source = TileOperationStatus.CACHED;
            else {
                // 索引命中后被删除/过期时重新下载，不把旧快照当作已有图片。
                beforeEncoding = loader.load(zoom, x, y, tile);
                tile.check();
                if (beforeEncoding == null || beforeEncoding.length == 0) source = TileOperationStatus.MISSING;
                else {
                    if (beforeEncoding.length > TileResourceLimits.getMaxTileBytes()) throw new IOException("源瓦片超过字节限制");
                    source = cache.put(query.getLayerName(), zoom, x, y, beforeEncoding, query.getFormat())
                            ? TileOperationStatus.DOWNLOADED : TileOperationStatus.FAILED;
                }
            }
            tile.check();
            status = source;
            if (source == TileOperationStatus.CACHED || source == TileOperationStatus.DOWNLOADED) {
                // 仅本张持有压缩前字节，不把数据挂在任务或长期线程上下文中。
                processed = pipeline.process(beforeEncoding, zoom, x, y);
                tile.check();
                status = processed.getResult().getState() == TileImageProcessingState.FAILED ? TileOperationStatus.FAILED : source;
                failure = processed.getFailure();
            }
        } catch (TileOperationException stopped) { status = stopped.getStatus(); failure = stopped; }
        catch (InterruptedException stopped) {
            Thread.currentThread().interrupt(); cancellation.cancel(); status = TileOperationStatus.CANCELLED; failure = stopped;
        } catch (Exception error) {
            failure = error;
            try { tile.check(); status = TileOperationStatus.FAILED; }
            catch (TileOperationException stopped) { status = stopped.getStatus(); failure = stopped; }
        }
        return new TilePrecacheTileResult(zoom, x, y, source == null ? status : source, status, processed, failure);
    }
}
