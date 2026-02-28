package cn.geoair.gtc.base.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.geoair.gtc.base.def.annotation.GaParameter;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface GaApi {

	static final String NULL = "";

	String name() default NULL;// 名称唯一键

	String text() default NULL;// 文本标题

	String alias() default NULL;// 别名

	String describe() default NULL;// 一段描述，大白话

	String[] tags() default NULL;// 标签分组

	boolean hidden() default false;// 是否隐藏

	GaParameter[] cfg() default {};// 参数

}
