package cn.geoair.map.dynamic.file.postgis;

import cn.geoair.map.dynamic.file.jdbc.AbstractJdbcGeoFileReader;

/** 使用 Core V2 / JDBC 公共层的 PostGIS 读取器。 */
public class PostgisJdbcGeoFileReader extends AbstractJdbcGeoFileReader {

    public PostgisJdbcGeoFileReader() {
        super(new PostgisJdbcGeoDialect());
    }
}
