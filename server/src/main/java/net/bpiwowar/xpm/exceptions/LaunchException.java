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

/**
 * Exception thrown when a process fails to start
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class LaunchException extends ExperimaestroException {
    public LaunchException() {
        super();
    }

    public LaunchException(String message, Throwable t) {
        super(message, t);
    }

    public LaunchException(Throwable t, String format, Object... values) {
        super(t, format, values);
    }

    public LaunchException(String message) {
        super(message);
    }

    public LaunchException(String format, Object... values) {
        super(format, values);
    }

    public LaunchException(Throwable t) {
        super(t);
    }
}
