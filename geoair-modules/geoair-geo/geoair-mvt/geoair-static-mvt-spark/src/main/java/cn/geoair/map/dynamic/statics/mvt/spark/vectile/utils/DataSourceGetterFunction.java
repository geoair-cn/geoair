package cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils;

import cn.geoair.base.lang.lambda.GkSerializableFunction;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;

import javax.sql.DataSource;

/**
 * 可插拔的 DataSource 创建函数。
 * <p>
 * 默认实现为 {@link DefaultDataSourceGetterFunction}，使用 JDBC DriverManager 创建连接。
 * 用户可通过自定义实现替换为连接池等方案。
 *
 * @author 张俊
 * @date 2026/6/18
 */
public interface DataSourceGetterFunction extends GkSerializableFunction<DataSourceConfig, DataSource> {
}
