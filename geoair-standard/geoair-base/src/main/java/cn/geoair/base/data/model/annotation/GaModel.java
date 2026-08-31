package cn.geoair.base.data.model.annotation;

import cn.geoair.base.data.model.applyer.GiModelApplyer;
import cn.geoair.base.def.annotation.GaParameter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface GaModel {

    static final String NULL = "";

    @Deprecated
    String name() default NULL;

    /** 名称 ,如果是PO对象，这个字段描述的就是数据库表的名称 */
    String tableName() default NULL;

    String text() default NULL; // 文本

    String alias() default NULL; // 别名

    String describe() default NULL; // 一段描述，大白话

    Class<? extends GiModelApplyer>[] applyer() default {};

    int tag() default 0;

    GaParameter[] cfg() default {}; // 参数
}
