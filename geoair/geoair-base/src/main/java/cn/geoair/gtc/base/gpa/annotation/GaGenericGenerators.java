package cn.geoair.gtc.base.gpa.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Array of generic generator definitions.
 *
 * @author Ray
 */
@Target({PACKAGE, TYPE})
@Retention(RUNTIME)
public @interface GaGenericGenerators {
	/**
	 * The aggregated generators.
	 */
	GaGenericGenerator[] value();
}
