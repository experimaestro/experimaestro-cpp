/*
 * This file is part of experimaestro.
 * Copyright (c) 2013 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.exceptions;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class XPMRhinoIllegalArgumentException extends XPMRhinoException {
    public XPMRhinoIllegalArgumentException() {
    }

    public XPMRhinoIllegalArgumentException(String message, Throwable t) {
        super(message, t);
    }

    public XPMRhinoIllegalArgumentException(Throwable t, String format, Object... values) {
        super(t, format, values);
    }

    public XPMRhinoIllegalArgumentException(Throwable t) {
        super(t);
    }

    public XPMRhinoIllegalArgumentException(String message) {
        super(message);
    }

    public XPMRhinoIllegalArgumentException(String format, Object... values) {
        super(format, values);
    }
}
