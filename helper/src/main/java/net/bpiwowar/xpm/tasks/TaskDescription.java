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
