package cn.geoair.comp.dynamic.ds.datasource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 创建人: 张俊 创建时间: 2025/1/6 16:39 描述: 动态数据源切换的实现类
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface GirDataSourceChange {

    GirDataSourceRwTypeEnum rwType() default GirDataSourceRwTypeEnum.可读可写;

    String groupName() default "default";
}
