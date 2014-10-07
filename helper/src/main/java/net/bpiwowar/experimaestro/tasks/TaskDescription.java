package net.bpiwowar.experimaestro.tasks;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by bpiwowar on 1/10/14.
 */
@Retention(RetentionPolicy.RUNTIME)

public @interface TaskDescription {
    String id();

    String output();

    String help() default "";

    /**
     * A list of registry classes
     *
     * @return A class
     */
    Class<?>[] registry() default {};
}
