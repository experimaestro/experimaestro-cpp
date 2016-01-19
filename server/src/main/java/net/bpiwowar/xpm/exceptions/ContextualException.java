package net.bpiwowar.xpm.exceptions;

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

import java.util.List;

import static java.lang.String.format;

/**
 * Exceptions that can be contextualized
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public interface ContextualException<T> {
    /**
     * Add a new context
     *
     * @see java.lang.String#format(String, Object...)
     * @param format A format string
     * @param values
     * @return
     */
    default T addContext(String format, Object... values) {
        getContext().add(format(format, values));
        return (T) this;
    }

    /**
     * Get the contexts
     * @return The list of contexts (the first one being the innermost)
     */
    List<String> getContext();
}
