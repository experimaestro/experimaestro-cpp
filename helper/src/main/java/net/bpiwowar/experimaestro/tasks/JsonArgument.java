package net.bpiwowar.experimaestro.tasks;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a JSON argument
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonArgument {
    /**
     * The name of the argument (leave empty if using the field name)
     * @return The name of the argument
     */
    String name() default "";

    /**
     * A description string
     * @return The description string
     */
    String help() default "";

    /**
     * Whether the argument is required
     * @return true if required, false otherwise
     */
    boolean required() default false;
}
