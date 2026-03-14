package cn.geoair.base.lang.invoke;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface GaMethodHandImpl {

	public String id() default "";

	public Class<?> implClass() default Object.class;

	public String implMethod() default "";

	public ImplType type() default ImplType.expectfirst;

	public static enum ImplType {

		expectfirst, // 期待优先
		cover, // 覆盖
		comity // 礼让

	}

}
