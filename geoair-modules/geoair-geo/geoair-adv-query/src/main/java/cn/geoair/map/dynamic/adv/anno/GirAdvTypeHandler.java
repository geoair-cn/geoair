package cn.geoair.map.dynamic.adv.anno;

import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/8/10
 * @description： 字段级类型处理器注解，指定该字段使用的类型转换器
 * <p>该处理器仅对当前字段生效，不注册到全局注册中心</p>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GirAdvTypeHandler {

    /**
     * 指定该字段使用的类型处理器实现类
     */
    Class<? extends AdvTypeHandler<?>> value();
}
