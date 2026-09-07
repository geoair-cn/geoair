package cn.geoair.map.dynamic.file.test.gj2jdbc;

import cn.geoair.map.dynamic.file.mysql.MysqlJdbcGeoFileWriter;
import cn.geoair.map.dynamic.file.mysql.MysqlJdbcWriterLinkInfo;

/**
 * GeoJSON 写入 MySQL 8 Spatial 的 Main 测试。
 *
 * <p>示例 VM options：
 * {@code -Dgeoair.file.geojson=E:\\data\\poi.geojson -Dgeoair.mysql.jdbc-url=jdbc:mysql://127.0.0.1:3306/gis
 * -Dgeoair.mysql.username=root -Dgeoair.mysql.password=123456 -Dgeoair.mysql.table=poi_mysql_test}</p>
 *
 * @author 张逢吉
 */
public class GeoJsonToMysql {

    public static void main(String[] args) {
        MysqlJdbcWriterLinkInfo linkInfo = new MysqlJdbcWriterLinkInfo();
        linkInfo.setJdbcUrl(GeoJsonToJdbcTransferSupport.requireProperty("geoair.mysql.jdbc-url"));
        linkInfo.setUsername(GeoJsonToJdbcTransferSupport.requireProperty("geoair.mysql.username"));
        linkInfo.setPassword(GeoJsonToJdbcTransferSupport.requireProperty("geoair.mysql.password"));
        linkInfo.setTableName(GeoJsonToJdbcTransferSupport.requireProperty("geoair.mysql.table"));
        GeoJsonToJdbcTransferSupport.setOptionalProperty("geoair.mysql.schema", linkInfo::setSchema);
        GeoJsonToJdbcTransferSupport.transfer("MySQL", linkInfo, new MysqlJdbcGeoFileWriter());
    }
}
