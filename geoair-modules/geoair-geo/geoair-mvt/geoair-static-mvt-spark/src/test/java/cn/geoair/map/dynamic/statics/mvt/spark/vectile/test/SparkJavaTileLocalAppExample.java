package cn.geoair.map.dynamic.statics.mvt.spark.vectile.test;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.ReadStrategy;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.SparkJavaTileLocalApp;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.hutool.core.util.IdUtil;

/**
 * @author ：张俊
 * @date ：Created in 2026/8/14 15:24
 * @description： TODO
 */
public class SparkJavaTileLocalAppExample {
    public static void main(String[] args) throws Exception {
        String inputUrl = "#jdbc:postgresql://postgres#tcsd1234/192.168.0.110:5432/jwyt_v3/flowable";
        String tableName = "t_" + IdUtil.getSnowflakeNextId();
        String outputUrl = "#jdbc:postgresql://postgres#tcsd1234/192.168.0.110:5432/jwyt_v3/flowable/" + tableName;

        TileSliceParameter parameter = new TileSliceParameter()
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
                .setInputSource(DataSourceConfig.fromProtocol(inputUrl))
                .setOutputSource(DataSourceConfig.fromProtocol(outputUrl));

        // 序列化
        String encoded = parameter.toBase32();
        System.out.println("[序列化]");
        System.out.println("  Base32 编码: " + encoded);
        System.out.println();

        // 反序列化
        TileSliceParameter decoded = TileSliceParameter.fromBase32(encoded);
        SparkJavaTileLocalApp.runByTileSliceParameter(decoded);
    }
}
