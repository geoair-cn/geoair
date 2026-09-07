package cn.geoair.map.dynamic.file.oracle;

import cn.geoair.map.dynamic.file.jdbc.AbstractJdbcGeoFileReader;

/** 使用 Core V2 / JDBC 公共层的 Oracle Spatial 读取器。 */
public class OracleJdbcGeoFileReader extends AbstractJdbcGeoFileReader {

    public OracleJdbcGeoFileReader() {
        super(new OracleJdbcGeoDialect());
    }
}
