package cn.geoair.map.dynamic.file.postgis;

import cn.geoair.map.dynamic.file.jdbc.link.JdbcGeoWriterLinkInfo;

/** Core V2 使用的 PostGIS 写入连接信息。 */
public class PostgisJdbcWriterLinkInfo extends JdbcGeoWriterLinkInfo {

    @Override
    protected String getDriverClassName() {
        return "org.postgresql.Driver";
    }
}
