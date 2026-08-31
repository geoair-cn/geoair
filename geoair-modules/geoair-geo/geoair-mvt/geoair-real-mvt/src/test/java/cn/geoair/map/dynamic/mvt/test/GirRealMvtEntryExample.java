package cn.geoair.map.dynamic.mvt.test;

import cn.geoair.map.dynamic.mvt.GirRealMvtHelper;
import cn.geoair.map.dynamic.mvt.dto.ParamCheckResult;
import cn.geoair.map.dynamic.mvt.dto.TileGlobalConfig;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;
import cn.geoair.map.dynamic.mvt.exec.ITileExecutor;
import cn.geoair.map.dynamic.mvt.exec.TileExecutorFactory;

/** geoair-real-mvt 实时入口示例 */
public class GirRealMvtEntryExample {

    public static void main(String[] args) {
        GirRealMvtHelper helper = GirRealMvtHelper.getInstance();
        TileRequestParams requestParams = new TileRequestParams();
        requestParams.setDsId("gis_ds");
        requestParams.setSchemaName("public");
        requestParams.setTbNameOrSql("road_layer");
        requestParams.setSrid("4326");
        requestParams.setGeomFieldName("geom");
        requestParams.setMinZoom(6);
        requestParams.setKeepFieldAll(true);

        ParamCheckResult result = helper.checkTileRequestParams(requestParams, "road_layer");
        ITileExecutor executor = TileExecutorFactory.getInstance(requestParams, "road_layer");
        TileGlobalConfig config = executor.getTileGlobalConfig();

        System.out.println("param check = " + result.isSuccess());
        System.out.println("config layerName = " + config.getLayerName());
        System.out.println("config version = " + config.getVersion());
    }
}
