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

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class ExperimaestroException extends Exception implements ContextualException<ExperimaestroException> {
    private static final long serialVersionUID = 1L;
    ArrayList<String> context = new ArrayList<>();

    public ExperimaestroException() {
        super();
    }

    public ExperimaestroException(String message, Throwable t) {
        super(message, t);
    }

    public ExperimaestroException(Throwable t, String format, Object... values) {
        super(String.format(format, values), t);
    }

    public ExperimaestroException(String message) {
        super(message);
    }

    public ExperimaestroException(String format, Object... values) {
        super(String.format(format, values));
    }

    public ExperimaestroException(Throwable t) {
        super(t);
    }

    public ExperimaestroException addContext(String string, Object... values) {
        context.add(format(string, values));
        return this;
    }

    public List<String> getContext() {
        return context;
    }

}
