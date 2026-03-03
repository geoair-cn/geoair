package cn.geoair.base.bean;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface GaBeanConfig {

	/**
	 * 实例名，在spring容器或者dubbo spi实现在多个接口实现中通过id或name确定唯一实例
	 * @return
	 */
	String value() default "";

	String scope() default "single";

	/**
	 * 设置默认实现类 需要有无参构造函数
	 * @return 默认实现类的Class对象
	 */
	Class<?> placeHolderClass() default Object.class;

	/**
	 * 设置默认实现类类名
	 * @return 默认实现类的完整类名
	 */
	String placeHolder() default "";

}
