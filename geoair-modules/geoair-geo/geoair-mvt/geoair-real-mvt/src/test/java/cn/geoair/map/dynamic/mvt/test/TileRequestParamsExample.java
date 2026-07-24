package cn.geoair.map.dynamic.mvt.test;

import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;

/** TileRequestParams 示例 */
public class TileRequestParamsExample {

    public static void main(String[] args) {
        TileRequestParams params = new TileRequestParams();
        params.setDsId("gis_ds");
        params.setSchemaName("public");
        params.setTbNameOrSql("road_layer");
        params.setSrid("4326");
        params.setGeomFieldName("geom");
        params.setMinZoom(6);
        params.setKeepFieldAll(true);

        String encoded = params.toBase32();
        TileRequestParams decoded = TileRequestParams.fromBase32(encoded);

        System.out.println("encoded = " + encoded);
        System.out.println("decoded dsId = " + decoded.getDsId());
        System.out.println("decoded geomFieldName = " + decoded.getGeomFieldName());
    }
}
