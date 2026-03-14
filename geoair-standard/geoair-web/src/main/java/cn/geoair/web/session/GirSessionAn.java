package cn.geoair.web.session;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface GirSessionAn {

	public static String NULL = "";

	public String cookieKey() default NULL;//

	public String tokenKey() default NULL;//

	public long httpTimeout() default 0;//

	public int cookieTimeout() default 0;//

	public long tokenTimeout() default 0;//

	public boolean useCache() default false;

	public boolean tokenInHeader() default false;

	public String catalog() default NULL;// 存储目录

	public String cacheName() default " gtcSessionCache";

}
