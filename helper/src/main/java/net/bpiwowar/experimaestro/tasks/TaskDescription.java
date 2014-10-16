package net.bpiwowar.experimaestro.tasks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Description of an Experimaestro task
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TaskDescription {
    /**
     * The ID of the task
     *
     * @return A string namespace:URI
     */
    String id();

    /**
     * The type of the output
     *
     * @return A string namespace:URI
     */
    String output();

    /**
     * Description of the task
     *
     * @return Help string
     */
    String description() default "";

    /**
     * A list of registry classes
     *
     * @return A list of class to check as an annotation registry
     */
    Class<?>[] registry() default {};
}
