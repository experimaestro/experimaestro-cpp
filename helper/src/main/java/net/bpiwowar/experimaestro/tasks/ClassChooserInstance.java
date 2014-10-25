package net.bpiwowar.experimaestro.tasks;

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
