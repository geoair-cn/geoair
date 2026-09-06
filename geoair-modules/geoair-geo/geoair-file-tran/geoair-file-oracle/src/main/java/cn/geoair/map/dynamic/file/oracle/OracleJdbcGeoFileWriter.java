package cn.geoair.map.dynamic.file.oracle;

import cn.geoair.map.dynamic.file.jdbc.AbstractJdbcGeoFileWriter;

/** 使用 Core V2 / JDBC 公共层的 Oracle Spatial 写入器。 */
public class OracleJdbcGeoFileWriter extends AbstractJdbcGeoFileWriter {

    public OracleJdbcGeoFileWriter() {
        super(new OracleJdbcGeoDialect());
    }
}
