package net.bpiwowar.experimaestro.tasks;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by bpiwowar on 1/10/14.
 */
@Retention(RetentionPolicy.RUNTIME)

public @interface TaskDescription {
    /**
     * The ID of the task
     * @return A string namespace:URI
     */
    String id();

    /**
     * The type of the output
     * @return A string namespace:URI
     */
    String output();

    /**
     * Description of the task
     * @return Help string
     */
    String help() default "";

    /**
     * A list of registry classes
     *
     * @return A class
     */
    Class<?>[] registry() default {};
}
