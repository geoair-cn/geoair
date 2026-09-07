package cn.geoair.map.dynamic.file.oracle;

import cn.geoair.map.dynamic.file.jdbc.link.JdbcGeoWriterLinkInfo;

/** Core V2 使用的 Oracle Spatial 写入连接信息。 */
public class OracleJdbcWriterLinkInfo extends JdbcGeoWriterLinkInfo {

    @Override
    protected String getDriverClassName() {
        return "oracle.jdbc.OracleDriver";
    }
}
