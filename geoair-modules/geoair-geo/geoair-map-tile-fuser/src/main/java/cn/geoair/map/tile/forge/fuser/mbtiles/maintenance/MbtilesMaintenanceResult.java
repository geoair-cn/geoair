package cn.geoair.map.tile.forge.fuser.mbtiles.maintenance;

import lombok.Value;

/** 实际占用和日志结果，不以BLOB减少量冒充物理磁盘回收。 */
@Value
public class MbtilesMaintenanceResult {
    MbtilesMaintenanceStatus status;
    MbtilesFileInfo before;
    MbtilesFileInfo after;
    MbtilesCheckpointResult checkpoint;
}
