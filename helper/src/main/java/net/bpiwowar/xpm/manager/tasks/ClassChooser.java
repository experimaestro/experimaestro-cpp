package net.bpiwowar.xpm.manager.tasks;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks object where the type can be selected through the $type attributes
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ClassChooser {
    /**
     * Look at inner classes
     * @return True if we should look at inner classes
     */
    boolean inner() default false;

    /**
     * The classes
     * @return The candidate classes
     */
    Class<?>[] classes() default {};

    /**
     * The packages (containing the classes)
     * @return Look all classes within the package of this class
     */
    Class<?>[] classesOfPackage() default {};

    /**
     * Given instances
     * @return Class chooser instance annotation
     */
    ClassChooserInstance[] instances() default {};

    /**
     * Mode for choosing the right instance (can be overridden by instances)
     * @return A default mode
     */
    ClassChooserMode mode() default ClassChooserMode.DEFAULT;
}
