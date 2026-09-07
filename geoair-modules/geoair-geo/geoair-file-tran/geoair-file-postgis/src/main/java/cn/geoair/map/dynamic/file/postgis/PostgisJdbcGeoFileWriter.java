package cn.geoair.map.dynamic.file.postgis;

import cn.geoair.map.dynamic.file.jdbc.AbstractJdbcGeoFileWriter;

/** 使用 Core V2 / JDBC 公共层的 PostGIS 写入器。 */
public class PostgisJdbcGeoFileWriter extends AbstractJdbcGeoFileWriter {

    public PostgisJdbcGeoFileWriter() {
        super(new PostgisJdbcGeoDialect());
    }
}
