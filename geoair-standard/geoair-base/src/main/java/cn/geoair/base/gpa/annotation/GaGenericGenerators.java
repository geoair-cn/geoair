package cn.geoair.base.gpa.annotation;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Array of generic generator definitions.
 *
 * @author Ray
 */
@Target({PACKAGE, TYPE})
@Retention(RUNTIME)
public @interface GaGenericGenerators {

    /** The aggregated generators. */
    GaGenericGenerator[] value();
}
