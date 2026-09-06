package cn.geoair.map.dynamic.file.mysql;

import cn.geoair.map.dynamic.file.jdbc.link.JdbcGeoWriterLinkInfo;

/** Core V2 使用的 MySQL 8 Spatial 写入连接信息。 */
public class MysqlJdbcWriterLinkInfo extends JdbcGeoWriterLinkInfo {

    @Override
    protected String getDriverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }
}
