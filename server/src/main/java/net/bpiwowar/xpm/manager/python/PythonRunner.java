package net.bpiwowar.xpm.manager.python;

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

import com.google.common.reflect.TypeToken;
import net.bpiwowar.xpm.connectors.LocalhostConnector;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.Repositories;
import net.bpiwowar.xpm.manager.scripting.*;
import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.utils.Functional;
import net.bpiwowar.xpm.utils.log.Logger;
import org.apache.log4j.Hierarchy;
import org.python.core.*;
import org.python.util.PythonInterpreter;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

/**
 * Global context when executing a javascript
 */
public class PythonRunner implements AutoCloseable {

    static private final Logger LOGGER = Logger.getLogger();

    private static Map<String, PythonType> TYPES;

    private static Map<Class, Constructor> WRAPPERS;

    private final Map<String, String> environment;

    private final StaticContext staticContext;

    private final PythonInterpreter interpreter;

    private final ScriptContext scriptContext;

    public PythonRunner(Map<String, String> environment, Repositories repositories, Scheduler scheduler,
                        Hierarchy loggerRepository, String pythonPath,
                        BufferedWriter out, BufferedWriter err) throws Exception {

        init();

        this.staticContext = new StaticContext(scheduler, loggerRepository)
                .repository(repositories);
        this.environment = environment;

        interpreter = PythonInterpreter.threadLocalStateInterpreter(null);
        PySystemState interpreterState = interpreter.getSystemState();
        for (String path : pythonPath.split(":")) {
            interpreterState.path.add(new PyString(path));
        }
        interpreter.setOut(out);
        interpreter.setErr(err);
        scriptContext = staticContext.scriptContext();

        LOGGER.trace("Identity = %s", Py.getSystemState() == interpreterState);

        // XPM module
        final PyModule xpmModule = imp.addModule("xpm");

        // Add classes
        for (PyType type : TYPES.values()) {
            xpmModule.__setattr__(type.getName(), type);
        }

        // Add constants
        Scripting.forEachConstant((name, value) -> {
            xpmModule.__setattr__(name, wrap(value));
        });

        // Add functions
        Scripting.forEachFunction(m -> {
            xpmModule.__setattr__(m.getKey(), new PythonMethod(null, m));
        });

        // Add Python specific functions
        for (MethodFunction m : Scripting.getMethodFunctions(PythonFunctions.class)) {
            xpmModule.__setattr__(m.getKey(), new PythonMethod(null, m));
        }

        // XPM object: wrap properties
        final XPM xpm = new XPM();
        ClassDescription xpmDescription = ClassDescription.analyzeClass(XPM.class);
        for (Map.Entry<Object, ArrayList<Method>> x : xpmDescription.getMethods().entrySet()) {
            final Object key = x.getKey();
            if (key instanceof String) {
                final MethodFunction methodFunction = new MethodFunction(key);
                methodFunction.add(x.getValue());
                xpmModule.__setattr__((String) key, new PythonMethod(xpm, methodFunction));
            } else {
                throw new XPMRuntimeException("Could not handle key ", key);
            }
        }

        // Add properties
        xpmModule.__setattr__("tasks", wrap(new Tasks()));
        xpmModule.__setattr__("logger", wrap(new ScriptingLogger("xpm")));
    }

    /**
     * Gather types, etc.
     */
    static private void init() {
        if (TYPES == null) {
            TYPES = new HashMap<>();
            WRAPPERS = new HashMap<>();
            Scripting.forEachType(Functional.propagate(aClass -> {
                final PythonType type = new PythonType(aClass);
                TYPES.put(type.getName(), type);
                if (WrapperObject.class.isAssignableFrom(aClass)) {
                    final Class<?> wrappedClass = (Class<?>) ((ParameterizedType) TypeToken.of(aClass)
                            .getSupertype(WrapperObject.class).getType()).getActualTypeArguments()[0];
                    final Constructor constructor = aClass.getConstructor(wrappedClass);
                    WRAPPERS.put(wrappedClass, constructor);
                }
            }));
        }
    }

    public static PyObject wrap(Object object) {
        // Simple case
        if (object == null)
            return Py.None;

        if (object instanceof PyObject)
            return (PyObject) object;


        final Class<?> objectClass = object.getClass();

        if (object.getClass().isArray()) {
            final PyList pyList = new PyList();
            for (int i = Array.getLength(object); --i >= 0; ) {
                pyList.add(wrap(Array.get(object, i)));
            }
            return pyList;
        }

        // Wrapper case
        // Wrapper case -- go up in the hierarchy
        for (Map.Entry<Class, Constructor> entry : WRAPPERS.entrySet()) {
            if (entry.getKey().isAssignableFrom(objectClass)) {
                try {
                    return new PythonObject(entry.getValue().newInstance(object));
                } catch (Exception e) {
                    throw new UnsupportedOperationException("Could not wrap object of class " + objectClass, e);
                }
            }
        }

        // Simple types
        if (object instanceof String) {
            return new PyString((String) object);
        }

        if (object instanceof Boolean) {
            return new PyBoolean(((Boolean) object).booleanValue());
        }

        if (object instanceof Long) {
            return new PyLong((long) object);
        }

        if (object instanceof Number) {
            return new PyFloat(((Number) object).doubleValue());
        }

        // Exposed objects
        final Exposed exposed = objectClass.getAnnotation(Exposed.class);
        if (exposed != null) {
            return new PythonObject(object);
        }

        // Map entry as tuple
        if (object instanceof Map.Entry) {
            Map.Entry entry = (Map.Entry) object;
            return new PyTuple(wrap(entry.getKey()), wrap(entry.getValue()));
        }

        // Entry set
        if (object instanceof Set) {
            Set set = (Set) object;

            final AbstractSet wrappedSet = new AbstractSet() {
                @Override
                public Iterator iterator() {
                    return new Iterator() {
                        Iterator iterator = set.iterator();

                        @Override
                        public boolean hasNext() {
                            return iterator.hasNext();
                        }

                        @Override
                        public Object next() {
                            return wrap(iterator.next());
                        }
                    };
                }

                @Override
                public int size() {
                    return 0;
                }
            };
            return new PySet(wrappedSet, null);
        }

        throw new IllegalArgumentException(format("Cannot wrap class %s into python object", objectClass));
    }

    public static Object unwrap(PyObject arg) {
        return arg.__tojava__(Object.class);
    }

    public static Object[] unwrap(PyObject[] args) {
        Object[] unwrapped = new Object[args.length];
        for (int i = 0; i < args.length; ++i) {
            unwrapped[i] = unwrap(args[i]);
        }
        return unwrapped;
    }

    @Override
    public void close() throws Exception {
        interpreter.close();
        scriptContext.close();
        staticContext.close();
    }

    public Object evaluateReader(LocalhostConnector connector, Path locator, FileReader reader, String filename, int lineno, Object security) throws Exception {
        ScriptContext.get().setCurrentScriptPath(Paths.get(filename));
        final PyCode code = interpreter.compile(reader, filename);
        return interpreter.eval(code);
    }

    public Object evaluateString(LocalhostConnector connector, Path locator, String content, String name, int lineno, Object security) throws Exception {
        final PyCode code = interpreter.compile(content, name);
        return interpreter.eval(code);
    }

}
