package cn.geoair.comp.message.converter.jts.jackson.auto;

import cn.geoair.comp.message.converter.jts.jackson.config.GirJtsJacksonAutoConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ：张俊
 * @date ：Created in 2026/3/19 18:59
 * @description： 启用JTS自动注册
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(GirJtsJacksonAutoConfig.class)
public @interface EnableGirJtsAutoRegister {
}
