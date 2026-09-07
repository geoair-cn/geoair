package cn.geoair.map.dynamic.file.test.gj2jdbc;

import cn.geoair.map.dynamic.file.oracle.OracleJdbcGeoFileWriter;
import cn.geoair.map.dynamic.file.oracle.OracleJdbcWriterLinkInfo;

/**
 * GeoJSON 写入 Oracle Spatial 的 Main 测试。
 *
 * <p>示例 VM options：
 * {@code -Dgeoair.file.geojson=E:\\data\\poi.geojson -Dgeoair.oracle.jdbc-url=jdbc:oracle:thin:@127.0.0.1:1521/orclpdb1
 * -Dgeoair.oracle.username=GIS -Dgeoair.oracle.password=123456 -Dgeoair.oracle.table=POI_ORACLE_TEST}</p>
 *
 * @author 张逢吉
 */
public class GeoJsonToOracle {

    public static void main(String[] args) {
        OracleJdbcWriterLinkInfo linkInfo = new OracleJdbcWriterLinkInfo();
        linkInfo.setJdbcUrl(GeoJsonToJdbcTransferSupport.requireProperty("geoair.oracle.jdbc-url"));
        linkInfo.setUsername(GeoJsonToJdbcTransferSupport.requireProperty("geoair.oracle.username"));
        linkInfo.setPassword(GeoJsonToJdbcTransferSupport.requireProperty("geoair.oracle.password"));
        linkInfo.setTableName(GeoJsonToJdbcTransferSupport.requireProperty("geoair.oracle.table"));
        GeoJsonToJdbcTransferSupport.setOptionalProperty("geoair.oracle.schema", linkInfo::setSchema);
        GeoJsonToJdbcTransferSupport.transfer("Oracle", linkInfo, new OracleJdbcGeoFileWriter());
    }
}
