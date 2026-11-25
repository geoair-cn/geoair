package cn.geoair.map.tile.forge.fuser.mbtiles;

import cn.geoair.map.tile.forge.fuser.execution.TileOperationException;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;

/** 显式维护占用，不能吞掉后作为缺片继续回源；HTTP状态由接入应用映射。 */
public class MbtilesMaintenanceException extends TileOperationException {
    public MbtilesMaintenanceException() {
        super(TileOperationStatus.FAILED, "MBTiles正在独占维护，请稍后重试");
    }
}
