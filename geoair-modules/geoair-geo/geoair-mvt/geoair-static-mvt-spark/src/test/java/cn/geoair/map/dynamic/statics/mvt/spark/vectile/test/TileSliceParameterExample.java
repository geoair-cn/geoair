package cn.geoair.map.dynamic.statics.mvt.spark.vectile.test;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.ReadStrategy;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;

/**
 * TileSliceParameter 示例
 */
public class TileSliceParameterExample {

    public static void main(String[] args) {
        TileSliceParameter parameter = new TileSliceParameter();
        parameter.setLayerName("road_layer")
            .setEdition("v1")
            .setGeomFieldName("geom")
            .setIdFieldName("id")
            .setReadStrategy(ReadStrategy.ID_PAGE)
            .setSourceDataSrid(4326)
            .setOutGridSrid(3857)
            .setMinZoom(6)
            .setMaxZoom(14)
            .setDropDensestAsNeeded(true)
            .setCoalesceDensestAsNeeded(true);

        String encoded = parameter.toBase32();
        TileSliceParameter decoded = TileSliceParameter.fromBase32(encoded);

        System.out.println("encoded = " + encoded);
        System.out.println("decoded layerName = " + decoded.getLayerName());
        System.out.println("decoded maxZoom = " + decoded.getMaxZoom());
    }
}
