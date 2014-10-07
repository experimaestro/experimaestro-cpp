package sf.net.experimaestro.tasks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark file
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Path {
    /**
     * Relative path from the output directory. By default, set to the field name.
     * @return A string with "/" separated components or empty
     */
    String value() default "";

    /**
     * Specify which JSON value should be set according to this path. By default,
     * set to the field name.
     * @return A valid json name or empty
     */
    String copy() default "";
}
