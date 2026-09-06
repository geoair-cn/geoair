package cn.geoair.map.dynamic.file.mysql;

import cn.geoair.map.dynamic.file.jdbc.AbstractJdbcGeoFileWriter;

/** 使用 Core V2 / JDBC 公共层的 MySQL 8 Spatial 写入器。 */
public class MysqlJdbcGeoFileWriter extends AbstractJdbcGeoFileWriter {

    public MysqlJdbcGeoFileWriter() {
        super(new MysqlJdbcGeoDialect());
    }
}
