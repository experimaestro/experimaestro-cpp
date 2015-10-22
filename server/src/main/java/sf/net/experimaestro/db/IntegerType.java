package sf.net.experimaestro.db;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface IntegerType {
    int bytes() default 2;
}
