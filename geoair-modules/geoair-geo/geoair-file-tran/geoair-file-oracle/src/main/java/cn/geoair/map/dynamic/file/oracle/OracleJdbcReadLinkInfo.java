package cn.geoair.map.dynamic.file.oracle;

import cn.geoair.map.dynamic.file.jdbc.link.JdbcGeoReadLinkInfo;

/** Core V2 使用的 Oracle Spatial 读取连接信息。 */
public class OracleJdbcReadLinkInfo extends JdbcGeoReadLinkInfo {

    @Override
    protected String getDriverClassName() {
        return "oracle.jdbc.OracleDriver";
    }
}
