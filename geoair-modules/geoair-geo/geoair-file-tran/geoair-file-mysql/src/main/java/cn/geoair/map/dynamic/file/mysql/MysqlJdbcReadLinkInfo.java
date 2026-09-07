package cn.geoair.map.dynamic.file.mysql;

import cn.geoair.map.dynamic.file.jdbc.link.JdbcGeoReadLinkInfo;

/** Core V2 使用的 MySQL 8 Spatial 读取连接信息。 */
public class MysqlJdbcReadLinkInfo extends JdbcGeoReadLinkInfo {

    @Override
    protected String getDriverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }
}
