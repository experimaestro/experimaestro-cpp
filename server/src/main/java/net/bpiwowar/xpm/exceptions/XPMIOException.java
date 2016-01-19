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


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * An illegal exception
 */
public class XPMIOException extends IOException implements ContextualException<XPMIOException> {
    public XPMIOException() {
        super();
    }

    public XPMIOException(String message, Throwable t) {
        super(message, t);
    }

    public XPMIOException(Throwable t, String format, Object... values) {
        super(format(format, values), t);
    }

    public XPMIOException(String message) {
        super(message);
    }

    public XPMIOException(String format, Object... values) {
        super(format(format, values));
    }

    public XPMIOException(Throwable t) {
        super(t);
    }

    List<String> context = new ArrayList<>();

    @Override
    public List<String> getContext() {
        return context;
    }

}
