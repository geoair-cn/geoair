package cn.geoair.comp.dynamic.ds.datasource;

import cn.geoair.comp.dynamic.ds.datasource.web.GirDataSourceWebContextWebConfig;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.context.annotation.Import;

/** 启用动态数据源的切换组件 web环境下 */
@Retention(RetentionPolicy.RUNTIME)
@Import(value = {GirDataSourceWebContextWebConfig.class, GirDynamicDataSourceAspect.class})
public @interface EnableDynamicWebDs {}
