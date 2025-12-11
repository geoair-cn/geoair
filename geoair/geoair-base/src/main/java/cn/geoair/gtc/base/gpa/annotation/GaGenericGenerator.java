package cn.geoair.gtc.base.gpa.annotation;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 *
 */
@Target({PACKAGE, TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Repeatable(GaGenericGenerators.class)
public @interface GaGenericGenerator {
	/**
	 * unique generator name.
	 */
	String name();
	/**
	 * Generator strategy or a fully qualified class name.
	 */
	String strategy();
	/**
	 * Optional generator parameters.
	 */
	//Parameter[] parameters() default {};
}
