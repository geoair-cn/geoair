package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.cache.TileCacheQuery;
import cn.geoair.map.tile.forge.fuser.execution.TileCancellationToken;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationException;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationScope;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;

import java.util.BitSet;
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

    TilePrecacheSegment(
            TilePrecacheLevel level,
            TileCacheQuery query,
            TilePrecacheCounter counter,
            TileCache cache,
            TileContentLoader loader,
            TileCancellationToken cancellation,
            TilePrecacheOptions options) {
        this.level = level;
        this.query = query;
        this.counter = counter;
        this.cache = cache;
        this.loader = loader;
        this.cancellation = cancellation;
        this.options = options;
    }

    public Void call() {
        TileOperationContext inspection =
                new TileOperationContext(cancellation, options.getInspectionTimeoutMillis());
        try (TileOperationScope ignored = inspection.bind()) {
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
            counter.add(TileOperationStatus.SKIPPED, query.size() - inside.cardinality());
            if (inside.isEmpty()) return null;
            BitSet existing = cache.findExisting(query, inspection);
            existing.and(inside);
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
            }
            return null;
        } finally {
            counter.publish();
        }
    }
}
