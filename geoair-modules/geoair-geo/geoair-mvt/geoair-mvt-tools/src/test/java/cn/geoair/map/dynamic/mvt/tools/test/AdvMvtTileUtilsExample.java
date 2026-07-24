package cn.geoair.map.dynamic.mvt.tools.test;

import cn.geoair.map.dynamic.mvt.tools.AdvMvtTileUtils;
import cn.geoair.map.dynamic.mvt.tools.param.TileExecParams;
import org.locationtech.jts.geom.Envelope;

/** geoair-mvt-tools 工具层示例 */
public class AdvMvtTileUtilsExample {

    public static void main(String[] args) {
        Envelope envelope4326 = AdvMvtTileUtils.getTileRect(10, 845, 388, 4326);
        Envelope envelope3857 = AdvMvtTileUtils.getTileRect(10, 845, 388, 3857);
        TileExecParams params =
                AdvMvtTileUtils.getTileExecParamsNotHasSql(10, 845, 388, 4326, 4326);

        System.out.println("tile rect 4326 = " + envelope4326);
        System.out.println("tile rect 3857 = " + envelope3857);
        System.out.println("tile exec params zoom = " + params.getZoom());
        System.out.println("tile exec params data srid = " + params.getSourceDataSrid());
    }
}
