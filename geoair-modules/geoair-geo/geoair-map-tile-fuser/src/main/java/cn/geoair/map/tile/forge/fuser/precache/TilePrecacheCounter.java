package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;

import java.util.EnumMap;
import java.util.function.Consumer;

/** 每个级别单独同步统计，回调按该级别的累计值顺序执行。回调应快速完成。 */
final class TilePrecacheCounter {
    private final int zoom;
    private final long total;
    private final EnumMap<TileOperationStatus, Long> counts =
            new EnumMap<>(TileOperationStatus.class);
    private final Consumer<TilePrecacheProgress> listener;
    private long lastNotice;

    TilePrecacheCounter(TilePrecacheLevel level, Consumer<TilePrecacheProgress> listener) {
        zoom = level.getZoom();
        total = level.getTotal();
        this.listener = listener;
        for (TileOperationStatus status : TileOperationStatus.values()) counts.put(status, 0L);
    }

    synchronized void add(TileOperationStatus status, long count) {
        counts.put(status, Math.addExact(counts.get(status), count));
        if (System.nanoTime() - lastNotice >= 250000000L) publish();
    }

    synchronized void publish() {
        lastNotice = System.nanoTime();
        if (listener != null) listener.accept(snapshot());
    }

    synchronized TilePrecacheProgress snapshot() {
        return new TilePrecacheProgress(zoom, total, counts);
    }
}
