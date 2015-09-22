package sf.net.experimaestro.manager.python;

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

import org.apache.commons.lang.NotImplementedException;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.ThreadState;
import sf.net.experimaestro.manager.Namespace;

import javax.xml.namespace.NamespaceContext;
import java.util.Iterator;

import static java.lang.String.format;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class PythonNamespaceContext implements NamespaceContext {
    private final ThreadState threadState;

    public PythonNamespaceContext() {
        threadState = Py.getThreadState();
    }

    @Override
    public String getNamespaceURI(String prefix) {
        final PyObject object = threadState.frame.getname(prefix);

        if (object == null) {
            return null;
        }

        if (object instanceof PythonObject) {
            PythonObject pythonObject = (PythonObject) object;
            if (pythonObject.object instanceof Namespace) {
                return ((Namespace) pythonObject.object).getURI();
            }

        }

        throw new NotImplementedException(format("Could not return namespace for %s", object.getType()));
    }

    @Override
    public String getPrefix(String namespaceURI) {
        throw new NotImplementedException();
    }

    @Override
    public Iterator getPrefixes(String namespaceURI) {
        throw new NotImplementedException();
    }

}
