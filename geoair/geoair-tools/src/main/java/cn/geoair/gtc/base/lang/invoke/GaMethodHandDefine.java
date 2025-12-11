package cn.geoair.gtc.base.lang.invoke;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface GaMethodHandDefine {
	public String id() default "";
	public String expectClassName() default "";
	public String expectMethodName() default "";

	//public boolean keepExpect() default false;
}
