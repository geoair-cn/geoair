package cn.geoair.comp.message.converter.jts.jackson.auto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * @author ：张俊
 * @date ：Created in 2026/3/19 18:59 @description： 用于以后拓展注册器
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(EnableGirJtsAutoRegister.class)
public @interface EnableGirAutoRegister {}
