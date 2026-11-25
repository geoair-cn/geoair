package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingState;
import cn.geoair.map.tile.forge.fuser.image.TileImageWriteState;

import lombok.Getter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** 不可变阶段累计；阶段数量互相重叠，不得相加作为整张处理进度。 */
@Getter
public final class TilePrecacheStages {
    private final Map<TileOperationStatus, Long> loads;
    private final Map<TileImageProcessingState, Long> processing;
    private final Map<TileImageWriteState, Long> writes;

    public TilePrecacheStages(
            Map<TileOperationStatus, Long> loads,
            Map<TileImageProcessingState, Long> processing,
            Map<TileImageWriteState, Long> writes) {
        EnumMap<TileOperationStatus, Long> loadCopy = new EnumMap<>(TileOperationStatus.class);
        loadCopy.putAll(loads);
        EnumMap<TileImageProcessingState, Long> processCopy =
                new EnumMap<>(TileImageProcessingState.class);
        processCopy.putAll(processing);
        EnumMap<TileImageWriteState, Long> writeCopy = new EnumMap<>(TileImageWriteState.class);
        writeCopy.putAll(writes);
        this.loads = Collections.unmodifiableMap(loadCopy);
        this.processing = Collections.unmodifiableMap(processCopy);
        this.writes = Collections.unmodifiableMap(writeCopy);
    }

    public static TilePrecacheStages empty() {
        return new TilePrecacheStages(
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    public long getChecked() {
        long count = 0;
        for (long value : processing.values()) count = Math.addExact(count, value);
        return count;
    }

    public long getStored() {
        return writes.getOrDefault(TileImageWriteState.STORED, 0L);
    }
}
