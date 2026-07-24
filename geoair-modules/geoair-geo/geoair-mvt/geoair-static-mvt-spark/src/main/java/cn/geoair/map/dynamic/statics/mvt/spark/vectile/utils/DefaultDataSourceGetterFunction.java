package cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils;

import cn.geoair.comp.dynamic.ds.simple.DriverManagerDataSource;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.PgConnectInfoSimple;

import java.util.Map;

import javax.sql.DataSource;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/18 17:11
 * @description： 默认的数据源获取器
 */
public class DefaultDataSourceGetterFunction implements DataSourceGetterFunction {

    @Override
    public DataSource apply(PgConnectInfoSimple pgConnectInfoSimple) {
        Map<String, String> params = pgConnectInfoSimple.toParams();
        return new DriverManagerDataSource(
                params.get("url"), params.get("user"), params.get("password"));
    }
}
