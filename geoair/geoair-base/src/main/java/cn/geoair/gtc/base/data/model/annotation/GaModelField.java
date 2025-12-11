package cn.geoair.gtc.base.data.model.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.geoair.gtc.base.data.common.GemNull;
import cn.geoair.gtc.base.def.annotation.GaParameter;
import cn.geoair.gtc.base.data.common.GemDatePattern;
import cn.geoair.gtc.base.data.model.applyer.GiModelFieldApplyer;


@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.TYPE_PARAMETER})
public @interface GaModelField {
	public static final String NULL = "";
	public String name() default NULL;//名称
	public String alias() default NULL;//别名
	public String text() default NULL;//文本
	public String describe() default NULL;//一段描述，大白话
//	public String describe() default NULL;//一段描述，大白话
	public boolean isID() default false;//是否主键
	public boolean isDisplay() default false;//是否是显示域
	public boolean isParentId() default false;//是否是属性父ID

    public Class<?> fk() default Object.class;//外键PO类
    public Class<? extends Enum<?>> em() default GemNull.class;//枚举类
    public GemDatePattern datePattern() default GemDatePattern.NULL;

    public String convert() default NULL;

	public Class<? extends GiModelFieldApplyer>[] applyer() default {};

	public String tar() default "";

	public String tag() default NULL;//标记

	public GaParameter[] cfg() default {};//参数
}



