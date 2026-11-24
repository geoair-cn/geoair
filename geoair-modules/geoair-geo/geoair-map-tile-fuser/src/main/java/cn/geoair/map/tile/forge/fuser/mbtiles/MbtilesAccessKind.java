package cn.geoair.map.tile.forge.fuser.mbtiles;

/** 短操作、完整后台任务和独占维护分别协调。 */
enum MbtilesAccessKind {
    OPERATION,
    TASK,
    MAINTENANCE
}
