package sf.net.experimaestro.manager.js;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 16/1/13
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface JSArgument {
    String name() default "";

    String type() default "";

    String help() default "";
}
