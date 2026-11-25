package cn.geoair.map.tile.forge.fuser.mbtiles.maintenance;

import lombok.Value;

/** SQLite报告的真实结果；执行SQL成功不等于日志已经全部合并。 */
@Value
public class MbtilesCheckpointResult {
    int busy;
    long logPages;
    long checkpointedPages;

    public boolean isWal() {
        return logPages >= 0;
    }

    public boolean isComplete() {
        return busy == 0 && logPages <= checkpointedPages;
    }
}
