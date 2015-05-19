package sf.net.experimaestro.manager.scripting;

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
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface Expose {
    /** The name of the function (by default, the name of the ) */
    String value() default "";

    /**
     * Marks a function that is used when the object is called
     */
    boolean call() default false;

    /**
     * Number of arguments that are optional.
     */
    int optional() default 0;

    /**
     * Optional arguments are at the beginning when true
     */
    boolean optionalsAtStart() default false;

    /**
     * Whether the context should be passed (language and script context)
     * @return True if the first argument should be a {@linkplain ScriptContext} object
     */
    boolean context() default false;

    /**
     * Whether this corresponds to an object property
     * @return
     */
    boolean property() default false;
}
