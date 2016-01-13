package sf.net.experimaestro.utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks classes for which the abstract adapter should be used
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonAbstract {
}
