package net.bpiwowar.experimaestro.tasks;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks object where the type can be selected through the $type attributes
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ClassChooser {
    /**
     * The classes
     */
    Class<?>[] classes() default {};

    /**
     * The packages (containing the classes)
     */
    Class<?>[] classesOfPackage() default {};

    /**
     * Given instances
     */
    ClassChooserInstance[] instances() default {};

}
