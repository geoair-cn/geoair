package cn.geoair.base.data.model.annotation;

import cn.geoair.base.data.common.GemDatePattern;
import cn.geoair.base.data.common.GemNull;
import cn.geoair.base.data.model.applyer.GiModelFieldApplyer;
import cn.geoair.base.def.annotation.GaParameter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE_PARAMETER})
public @interface GaModelField {

    static final String NULL = "";

    @Deprecated
    String name() default NULL;

    /** 名称 ,如果是PO对象，这个字段描述的就是数据库字段名称 */
    String columnName() default NULL;

    String alias() default NULL; // 别名

    String text() default NULL; // 文本

    String describe() default NULL; // 一段描述，大白话

    // public String describe() default NULL;//一段描述，大白话

    boolean isID() default false; // 是否主键

    boolean isDisplay() default false; // 是否是显示域

    boolean isParentId() default false; // 是否是属性父ID

    Class<?> fk() default Object.class; // 外键PO类

    Class<? extends Enum<?>> em() default GemNull.class; // 枚举类

    GemDatePattern datePattern() default GemDatePattern.NULL;

    String convert() default NULL;

    Class<? extends GiModelFieldApplyer>[] applyer() default {};

    String tar() default "";

    String tag() default NULL; // 标记

    GaParameter[] cfg() default {}; // 参数
}
