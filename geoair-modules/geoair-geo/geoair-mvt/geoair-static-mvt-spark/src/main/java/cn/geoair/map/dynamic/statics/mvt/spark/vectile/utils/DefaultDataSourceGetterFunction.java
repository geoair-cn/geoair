package cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils;

import cn.geoair.comp.dynamic.ds.simple.DriverManagerDataSource;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;

import javax.sql.DataSource;

/**
 * 默认的 DataSource 创建实现：通过 JDBC DriverManager 创建连接。
 *
 * @author 张俊
 * @date 2026/6/18
 */
public class DefaultDataSourceGetterFunction implements DataSourceGetterFunction {

    @Override
    public DataSource apply(DataSourceConfig config) {
        return new DriverManagerDataSource(
                config.getJdbcUrl(), config.getUsername(), config.getPassword());
    }
}
