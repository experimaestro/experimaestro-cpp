package sf.net.experimaestro.db;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks an index
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Indexes {
    Index[] value();
}
