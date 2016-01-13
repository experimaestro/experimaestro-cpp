package sf.net.experimaestro.exceptions;

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

import static java.lang.String.format;

/**
 * Exit exception
 */
public class ExitException extends XPMRhinoException {
    final private int code;

    public ExitException(int code) {
        super(format("exit called (%d)", code));
        this.code = code;
    }

    public ExitException(int code, String message, Object... objects) {
        super(format(message, objects));
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
