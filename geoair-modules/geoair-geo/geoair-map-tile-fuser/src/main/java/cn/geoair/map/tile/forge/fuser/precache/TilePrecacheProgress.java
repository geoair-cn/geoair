package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;

import lombok.Getter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** 不可变的级别快照，候选总数一次计算；处理数包含已有、下载、跳过、缺失和失败。 */
@Getter
@GaModel(text = "级别预缓存进度")
public final class TilePrecacheProgress {
    @GaModelField(text = "级别")
    private final int zoom;

    @GaModelField(text = "候选瓦片总数")
    private final long total;

    @GaModelField(text = "各处理结果数量")
    private final Map<TileOperationStatus, Long> counts;

    @GaModelField(text = "各阶段累计，不重复计入整体进度")
    private final TilePrecacheStages stages;

    public TilePrecacheProgress(int zoom, long total, Map<TileOperationStatus, Long> counts) {
        this(zoom, total, counts, TilePrecacheStages.empty());
    }

    public TilePrecacheProgress(
            int zoom,
            long total,
            Map<TileOperationStatus, Long> counts,
            TilePrecacheStages stages) {
        this.zoom = zoom;
        this.total = total;
        this.counts = Collections.unmodifiableMap(new EnumMap<>(counts));
        this.stages = Objects.requireNonNull(stages, "阶段进度不能为空");
    }

    public int getPercent() {
        if (total == 0) return 0;
        long done = getProcessed();
        return done >= total ? 100 : Math.min(99, (int) (100d * done / total));
    }

    public long getProcessed() {
        long value = 0;
        for (long count : counts.values()) value += count;
        return value;
    }

    public long getRemaining() {
        return total - getProcessed();
    }
}
