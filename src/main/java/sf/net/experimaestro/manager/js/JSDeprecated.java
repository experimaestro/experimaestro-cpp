package sf.net.experimaestro.manager.js;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a javascript function or constructor which is deprecated
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface JSDeprecated {
}
