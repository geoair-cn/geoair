package cn.geoair.map.dynamic.mvt.test;

import cn.geoair.map.dynamic.mvt.dto.TileExecutorConfig;
import cn.geoair.map.dynamic.mvt.dto.TileGlobalConfig;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;
import cn.geoair.map.dynamic.mvt.tools.param.TileExecParams;
import com.alibaba.fastjson2.JSONObject;

/**
 * TileGlobalConfig 示例
 */
public class TileGlobalConfigExample {

    public static void main(String[] args) {
        TileRequestParams requestParams = new TileRequestParams();
        requestParams.setDsId("gis_ds");
        requestParams.setGeomFieldName("geom");

        TileExecParams tileExecParams = new TileExecParams();
        tileExecParams.setZoom(10).setX(845).setY(388).setGridSrid(4326).setSourceDataSrid(4326);

        TileExecutorConfig executorConfig = new TileExecutorConfig();
        executorConfig.setIgnoreMinZoom(true);

        TileGlobalConfig config = new TileGlobalConfig()
            .setLayerName("road_layer")
            .setVersion(2)
            .setTileRequestParams(requestParams)
            .setTileExecParams(tileExecParams)
            .setTileExecConfig(executorConfig)
            .setCustomVariable(new JSONObject());

        System.out.println("layerName = " + config.getLayerName());
        System.out.println("version = " + config.getVersion());
        System.out.println("zoom = " + config.getTileExecParams().getZoom());
        System.out.println("geomFieldName = " + config.getTileRequestParams().getGeomFieldName());
    }
}
