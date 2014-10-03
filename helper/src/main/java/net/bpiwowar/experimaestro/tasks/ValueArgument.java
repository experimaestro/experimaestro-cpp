package net.bpiwowar.experimaestro.tasks;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by bpiwowar on 1/10/14.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ValueArgument {
    String name();

    String help() default "";

    boolean required() default false;

    String type();
}
