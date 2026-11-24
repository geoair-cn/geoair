package cn.geoair.map.tile.forge.fuser.mbtiles.statistics;

/** 持久化统计快照的有效性状态。 */
public enum MbtilesScanState {
    RUNNING, COMPLETED, STALE, INTERRUPTED
}
