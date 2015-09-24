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

import com.google.common.reflect.TypeToken;
import org.apache.log4j.Hierarchy;
import org.python.core.*;
import org.python.util.PythonInterpreter;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.manager.Repositories;
import sf.net.experimaestro.manager.scripting.*;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.Functional;
import sf.net.experimaestro.utils.log.Logger;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

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

    private final RunningContext runningContext;


    public PythonRunner(Map<String, String> environment, Repositories repositories, Scheduler scheduler,
                        Hierarchy loggerRepository,
                        BufferedWriter out, BufferedWriter err) throws Exception {

        init();

        this.staticContext = new StaticContext(scheduler, loggerRepository)
                .repository(repositories);
        this.environment = environment;
        interpreter = new PythonInterpreter();
        interpreter.setOut(out);
        interpreter.setErr(err);
        scriptContext = staticContext.scriptContext();
        runningContext = new RunningContext();

        Scripting.forEachFunction(m -> interpreter.set(m.getKey(), new PythonMethod(null, m)));
        for (MethodFunction m : Scripting.getMethodFunctions(PythonFunctions.class)) {
            interpreter.set(m.getKey(), new PythonMethod(null, m));
        }

        // Add classes
        for (PyType type : TYPES.values()) {
            interpreter.set(type.getName(), type);
        }

        // Add properties
        interpreter.set("tasks", wrap(new Tasks()));
        interpreter.set("logger", wrap(new ScriptingLogger("xpm")));
        interpreter.set("xpm", wrap(new XPM()));
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

        // Exposed objects
        final Exposed exposed = objectClass.getAnnotation(Exposed.class);
        if (exposed != null) {
            return new PythonObject(object);
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
        runningContext.close();
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
