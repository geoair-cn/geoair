package cn.geoair.comp.dynamic.ds.datasource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.context.annotation.Import;

/** 启用动态数据源的切换组件 */
@Retention(RetentionPolicy.RUNTIME)
@Import(value = {GirDynamicDataSourceAspect.class})
public @interface EnableDynamicDs {}
