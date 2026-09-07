package cn.geoair.map.dynamic.file.test.gj2jdbc;

import cn.geoair.map.dynamic.file.postgis.PostgisJdbcGeoFileWriter;
import cn.geoair.map.dynamic.file.postgis.PostgisJdbcWriterLinkInfo;

/**
 * GeoJSON 写入 PostGIS 的 Core V2 Main 测试。
 *
 * <p>示例 VM options：
 * {@code -Dgeoair.file.geojson=E:\\data\\poi.geojson -Dgeoair.postgis.jdbc-url=jdbc:postgresql://127.0.0.1:5432/gis
 * -Dgeoair.postgis.username=postgres -Dgeoair.postgis.password=123456 -Dgeoair.postgis.table=poi_postgis_test
 * -Dgeoair.postgis.schema=public}</p>
 *
 * @author 张逢吉
 */
public class GeoJsonToPostgis {

    public static void main(String[] args) {
        PostgisJdbcWriterLinkInfo linkInfo = new PostgisJdbcWriterLinkInfo();
        linkInfo.setJdbcUrl(GeoJsonToJdbcTransferSupport.requireProperty("geoair.postgis.jdbc-url"));
        linkInfo.setUsername(GeoJsonToJdbcTransferSupport.requireProperty("geoair.postgis.username"));
        linkInfo.setPassword(GeoJsonToJdbcTransferSupport.requireProperty("geoair.postgis.password"));
        linkInfo.setTableName(GeoJsonToJdbcTransferSupport.requireProperty("geoair.postgis.table"));
        GeoJsonToJdbcTransferSupport.setOptionalProperty("geoair.postgis.schema", linkInfo::setSchema);
        GeoJsonToJdbcTransferSupport.transfer("PostGIS", linkInfo, new PostgisJdbcGeoFileWriter());
    }
}
