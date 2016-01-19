/*
 *
 *  * This file is part of experimaestro.
 *  * Copyright (c) 2016 B. Piwowarski <benjamin@bpiwowar.net>
 *  *
 *  * experimaestro is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * experimaestro is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package net.bpiwowar.xpm.exceptions;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * SQL Exception
 */
public class XPMSQLException extends SQLException implements ContextualException<XPMSQLException> {
    public XPMSQLException() {
        super();
    }

    public XPMSQLException(String message, Throwable t) {
        super(message, t);
    }

    public XPMSQLException(Throwable t, String format, Object... values) {
        super(format(format, values), t);
    }

    public XPMSQLException(String message) {
        super(message);
    }

    public XPMSQLException(String format, Object... values) {
        super(format(format, values));
    }

    public XPMSQLException(Throwable t) {
        super(t);
    }

    List<String> context = new ArrayList<>();

    @Override
    public List<String> getContext() {
        return context;
    }
}
