package sf.net.experimaestro.manager.js;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks properties within JSBaseObject javascript objects
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface JSProperty {
    String value() default "";
}
