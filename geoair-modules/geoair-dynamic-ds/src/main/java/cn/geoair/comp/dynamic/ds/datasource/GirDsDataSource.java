package cn.geoair.comp.dynamic.ds.datasource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 创建人: 张俊 创建时间: 2025/1/6 16:39 描述: 动态数据源切换的实现类 由于名称起的比较具有迷惑性，故新增了一个注解 GirDataSourceChange
 * 使用上尽量迁移到GirDataSourceChange这个注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface GirDsDataSource {

    GirDataSourceRwTypeEnum rwType() default GirDataSourceRwTypeEnum.可读可写;

    String groupName() default "default";
}
