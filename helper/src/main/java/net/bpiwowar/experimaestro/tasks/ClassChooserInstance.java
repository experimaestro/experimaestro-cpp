package net.bpiwowar.experimaestro.tasks;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks object where the type can be selected through the $type attributes
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ClassChooserInstance {
    /** Name of the option
     * @return A string, if empty => name = qualified class name
     */
    String name() default "";

    /** Give the class of the instances
     * @return Object.class when using the annotated class
     */
    Class<?> instance() default Object.class;

    /**
     * Documentation
     * @return A description string
     */
    String description() default "";
}
