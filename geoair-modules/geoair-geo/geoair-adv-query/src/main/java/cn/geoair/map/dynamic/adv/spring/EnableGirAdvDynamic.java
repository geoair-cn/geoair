package cn.geoair.map.dynamic.adv.spring;

import java.lang.annotation.*;
import org.springframework.context.annotation.Import;

/** 启用 ADV 动态数据源自动装配 使用方式：在启动类 / 配置类上添加 @EnableGirAdvDynamic */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AdvAutoConfiguration.class) // 核心：自动导入你的自动配置类
public @interface EnableGirAdvDynamic {}
