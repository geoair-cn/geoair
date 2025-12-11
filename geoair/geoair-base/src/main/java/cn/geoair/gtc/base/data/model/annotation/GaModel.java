package cn.geoair.gtc.base.data.model.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


import cn.geoair.gtc.base.def.annotation.GaParameter;
import cn.geoair.gtc.base.data.model.applyer.GiModelApplyer;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface GaModel {
	public static final String NULL = "";

	public String name() default NULL;
	public String text() default NULL;//文本
    public String alias() default NULL;//别名
    public String describe() default NULL;//一段描述，大白话

    public Class<? extends GiModelApplyer>[] applyer() default {};

	public int tag() default 0;

	public GaParameter[] cfg() default {};//参数

}



