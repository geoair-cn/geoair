package cn.geoair.map.dynamic.statics.mvt.spark.vectile.test;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.ReadStrategy;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v2.SparkJavaTileLocalAppV2;
import cn.hutool.core.util.IdUtil;

/**
 * @author ：张俊
 * @date ：Created in 2026/8/14 15:24
 * @description： TODO
 */
public class SparkJavaTileLocalAppV2Example {
    public static void main(String[] args) throws Exception {
        String inputUrl = "#jdbc:postgresql://postgres#secret/10.0.0.1:5432/jwyt_v3/flowable";
        String tableName = "t_" + IdUtil.getSnowflakeNextId();
        String outputUrl =
                "#jdbc:postgresql://postgres#secret/10.0.0.1:5432/jwyt_v3/flowable/" + tableName;

        TileSliceParameter parameter =
                new TileSliceParameter()
                        .setLayerName("amap_traffic_conditions")
                        .setEdition("v1")
                        .setGeomFieldName("geom")
                        .setIdFieldName("link_id")
                        .setReadStrategy(ReadStrategy.ID_PAGE)
                        .setSourceDataSrid(4326)
                        .setOutGridSrid(3857)
                        .setMinZoom(6)
                        .setMaxZoom(14)
                        .setDropDensestAsNeeded(true)
                        .setCoalesceDensestAsNeeded(true)
                        .setQueryStatement("SELECT * FROM flowable.\"amap_traffic_conditions\"")
                        .setInputSource(DataSourceConfig.fromProtocolUrlStr(inputUrl))
                        .setOutputSource(DataSourceConfig.fromProtocolUrlStr(outputUrl));

        // 序列化
        String encoded = parameter.toBase32();
        System.out.println("[序列化]");
        System.out.println("  Base32 编码: " + encoded);
        System.out.println();

        // 反序列化
        TileSliceParameter decoded = TileSliceParameter.fromBase32(encoded);
        SparkJavaTileLocalAppV2.runByTileSliceParameter(decoded);
    }
}
