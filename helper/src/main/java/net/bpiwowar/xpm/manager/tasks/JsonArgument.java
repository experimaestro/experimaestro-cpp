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

import net.bpiwowar.xpm.manager.Constants;

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
     * The default value in JSON
     * @return The default value string
     */
    String value() default "";

    /**
     * Type of the JSON argument
     * @return the structured qualified type
     */
    String type() default "any";

    /**
     * Whether the argument is required
     * @return true if required, false otherwise
     */
    boolean required() default false;

    /**
     * Name of the argument in the output structure
     * @return The name
     */
    String copyTo() default "";
}
