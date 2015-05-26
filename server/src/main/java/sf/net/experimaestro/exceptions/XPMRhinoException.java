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

import org.mozilla.javascript.EvaluatorException;
import sf.net.experimaestro.manager.scripting.Exposed;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
public class XPMRhinoException extends EvaluatorException implements ContextualException<XPMRhinoException> {
    private static final long serialVersionUID = 1L;
    ArrayList<String> context = new ArrayList<>();

    public XPMRhinoException() {
        super("");
    }

    public XPMRhinoException(String message, Throwable t) {
        super(message);
        initCause(t);
    }

    public XPMRhinoException(Throwable t, String format, Object... values) {
        super(String.format(format, values));
        initCause(t);
    }

    public XPMRhinoException(Throwable t) {
        super("Wrapped exception");
        initCause(t);
    }

    public XPMRhinoException(String message) {
        super(message);
    }

    public XPMRhinoException(String format, Object... values) {
        super(String.format(format, values));
    }


    @Override
    public XPMRhinoException addContext(String string, Object... values) {
        context.add(format(string, values));
        return this;
    }

    @Override
    public List<String> getContext() {
        return context;
    }

}
