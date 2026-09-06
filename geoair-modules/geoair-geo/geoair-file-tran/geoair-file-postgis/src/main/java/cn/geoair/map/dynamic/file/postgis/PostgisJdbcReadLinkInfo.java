package cn.geoair.map.dynamic.file.postgis;

import cn.geoair.map.dynamic.file.jdbc.link.JdbcGeoReadLinkInfo;

/** Core V2 使用的 PostGIS 读取连接信息。 */
public class PostgisJdbcReadLinkInfo extends JdbcGeoReadLinkInfo {

    @Override
    protected String getDriverClassName() {
        return "org.postgresql.Driver";
    }
}
