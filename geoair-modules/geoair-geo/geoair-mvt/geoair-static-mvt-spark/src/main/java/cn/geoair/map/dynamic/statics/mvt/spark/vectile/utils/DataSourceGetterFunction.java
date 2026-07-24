package cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils;

import cn.geoair.base.lang.lambda.GkSerializableFunction;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.PgConnectInfoSimple;

import javax.sql.DataSource;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/18 17:11
 * @description： 默认的数据源获取器
 */
public interface DataSourceGetterFunction
        extends GkSerializableFunction<PgConnectInfoSimple, DataSource> {}
