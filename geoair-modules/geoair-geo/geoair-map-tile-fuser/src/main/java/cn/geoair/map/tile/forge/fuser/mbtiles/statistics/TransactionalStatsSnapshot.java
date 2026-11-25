package cn.geoair.map.tile.forge.fuser.mbtiles.statistics;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/** 文件内事务索引的快照；坐标范围标记 dirty 时需要重新计算。 */
@Getter
public final class TransactionalStatsSnapshot {
    private final boolean complete;
    private final long scanned;
    private final List<TransactionalStatsLevel> levels;

    public TransactionalStatsSnapshot(
            boolean complete, long scanned, List<TransactionalStatsLevel> levels) {
        this.complete = complete;
        this.scanned = scanned;
        this.levels = Collections.unmodifiableList(levels);
    }
}
