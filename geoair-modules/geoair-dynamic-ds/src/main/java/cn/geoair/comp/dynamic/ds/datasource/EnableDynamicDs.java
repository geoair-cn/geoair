package cn.geoair.comp.dynamic.ds.datasource;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 启用动态数据源的切换组件
 */
@Retention(RetentionPolicy.RUNTIME)
@Import(GirDynamicDataSourceAspect.class)
public @interface EnableDynamicDs {
}
