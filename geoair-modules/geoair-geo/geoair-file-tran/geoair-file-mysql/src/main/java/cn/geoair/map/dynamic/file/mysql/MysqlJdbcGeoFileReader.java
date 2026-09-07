package cn.geoair.map.dynamic.file.mysql;

import cn.geoair.map.dynamic.file.jdbc.AbstractJdbcGeoFileReader;

/** 使用 Core V2 / JDBC 公共层的 MySQL 8 Spatial 读取器。 */
public class MysqlJdbcGeoFileReader extends AbstractJdbcGeoFileReader {

    public MysqlJdbcGeoFileReader() {
        super(new MysqlJdbcGeoDialect());
    }
}
