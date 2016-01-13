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

/**
 * Thrown by a closeable
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 27/1/13
 */
public class CloseException extends ExperimaestroException {
    public CloseException() {
    }

    public CloseException(String message, Throwable t) {
        super(message, t);
    }

    public CloseException(Throwable t, String format, Object... values) {
        super(t, format, values);
    }

    public CloseException(String message) {
        super(message);
    }

    public CloseException(String format, Object... values) {
        super(format, values);
    }

    public CloseException(Throwable t) {
        super(t);
    }
}
