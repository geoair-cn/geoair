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

    public static final String NULL = "";

    @Deprecated
    public String name() default NULL;

    /** 名称 ,如果是PO对象，这个字段描述的就是数据库表的名称 */
    public String tableName() default NULL;

    public String text() default NULL; // 文本

    public String alias() default NULL; // 别名

    public String describe() default NULL; // 一段描述，大白话

    public Class<? extends GiModelApplyer>[] applyer() default {};

    public int tag() default 0;

    public GaParameter[] cfg() default {}; // 参数
}
