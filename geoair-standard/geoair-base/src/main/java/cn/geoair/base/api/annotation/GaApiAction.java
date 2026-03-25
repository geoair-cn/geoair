package cn.geoair.base.api.annotation;

import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.def.annotation.GaParameter;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface GaApiAction {

    static final String NULL = "";

    String name() default NULL; // 名称唯一键

    String text() default NULL; // 文本标题

    String alias() default NULL; // 别名

    String notes() default NULL; // 发布说明

    String[] tags() default ""; // 标签分组

    boolean hidden() default false; // 是否隐藏

    Class<?> response() default Void.class; // 返回结果集类型

    Class<? extends GiPager> pager() default GiPager.class; // 分页对象类型

    GaParameter[] cfg() default {}; // 参数
}
