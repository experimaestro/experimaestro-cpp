package sf.net.experimaestro.manager.js;

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

import bpiwowar.argparser.utils.Introspection;
import com.google.common.reflect.TypeToken;
import org.apache.log4j.Hierarchy;
import org.eclipse.wst.jsdt.debug.rhino.debugger.RhinoDebugger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Ref;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import sf.net.experimaestro.manager.Repositories;
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.manager.scripting.PropertyAccess;
import sf.net.experimaestro.manager.scripting.ScriptContext;
import sf.net.experimaestro.manager.scripting.Scripting;
import sf.net.experimaestro.manager.scripting.ScriptingReference;
import sf.net.experimaestro.manager.scripting.StaticContext;
import sf.net.experimaestro.manager.scripting.Wrapper;
import sf.net.experimaestro.manager.scripting.WrapperObject;
import sf.net.experimaestro.manager.scripting.XPM;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.Functional;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * Global context when executing a javascript
 */
public class JavaScriptRunner implements AutoCloseable {
    static private final Logger LOGGER = Logger.getLogger();

    static private Scriptable XPM_SCOPE;

    private static Map<Class, Constructor> WRAPPERS = new HashMap<>();

    final ScriptContext scriptContext;

    final Scriptable scope;

    private final RhinoDebugger debugger;

    final private Context context;

    public JavaScriptRunner(Repositories repositories, Scheduler scheduler, Hierarchy loggerRepository, Integer debugPort) throws Exception {
        StaticContext staticContext = new StaticContext(scheduler, loggerRepository).repository(repositories);
        // --- Debugging via JSDT
        // http://wiki.eclipse.org/JSDT/Debug/Rhino/Embedding_Rhino_Debugger#Example_Code
        ContextFactory factory = new ContextFactory();

        if (debugPort != null) {
            debugger = new RhinoDebugger("transport=socket,suspend=y,address=" + debugPort);
            debugger.start();
            factory.addListener(debugger);
        } else {
            debugger = null;
        }
        context = factory.enterContext();

        context.setWrapFactory(JSBaseObject.XPMWrapFactory.INSTANCE);
        scriptContext = staticContext.scriptContext();

        // Create scope
        scope = Context.getCurrentContext().newObject(init());
        scope.setPrototype(XPM_SCOPE);
        scope.setParentScope(null);

        Scripting.forEachObject((name, value) -> {
            ScriptableObject.defineProperty(scope, name, wrap(context, scope, value), 0);
        });
    }

    /**
     * Get the scope where all the main objects are defined
     */
    synchronized public static Scriptable init() throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {

        if (XPM_SCOPE == null) {
            ContextFactory factory = new ContextFactory();
            Context context = factory.enterContext();
            // Sealed object
            XPM_SCOPE = context.initStandardObjects(null, true);


            // --- Define functions and classes

            // Define the new classes (scans the package for implementations of ScriptableObject)
            ArrayList<Class<?>> list = new ArrayList<>();

            // Scan our classes
            try {
                final String packageName = JavaScriptRunner.class.getPackage().getName();
                final String resourceName = packageName.replace('.', '/');
                final Enumeration<URL> urls = XPM.class.getClassLoader().getResources(resourceName);
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    Introspection.addClasses(
                            aClass -> (ScriptableObject.class.isAssignableFrom(aClass)
                                    || JSConstructable.class.isAssignableFrom(aClass)
                                    || JSBaseObject.class.isAssignableFrom(aClass))
                                    && ((aClass.getModifiers() & Modifier.ABSTRACT) == 0)
                                    && !JavaScriptObject.class.equals(aClass),
                            list, packageName, -1, url);
                }
            } catch (IOException e) {
                LOGGER.error(e, "While trying to grab resources");
            }

            // Add the classes to javascript
            for (Class<?> aClass : list) {
                JSBaseObject.defineClass(XPM_SCOPE, aClass);
            }

            Scripting.forEachType(Functional.propagate(aClass -> {
                JSBaseObject.defineClass(XPM_SCOPE, aClass);
                if (WrapperObject.class.isAssignableFrom(aClass)) {
                    final Class<?> wrappedClass = (Class<?>) ((ParameterizedType) TypeToken.of(aClass)
                            .getSupertype(WrapperObject.class).getType()).getActualTypeArguments()[0];
                    final Constructor<?> constructor = aClass.getConstructor(wrappedClass);
                    WRAPPERS.put(wrappedClass, constructor);
                }
            }));

            Scripting.forEachConstant((name, value) -> {
                ScriptableObject.defineProperty(XPM_SCOPE, name, wrap(context, XPM_SCOPE, value), 0);
            });

            Scripting.forEachFunction(m -> ScriptableObject.defineProperty(XPM_SCOPE, m.getKey(), new JavaScriptFunction(m), 0));


            Context.exit();
        }
        return XPM_SCOPE;
    }

    static void addNewObject(Context cx, Scriptable scope,
                             final String objectName, final String className,
                             final Object[] params) {

        ScriptableObject.defineProperty(scope, objectName, cx.newObject(scope, className, params), 0);
    }

    /**
     * Wraps a new object into a JavaScript object
     *
     * @param jcx
     * @param object The object to wrap
     * @return The wrapped object
     */
    public static Object wrap(JavaScriptContext jcx, Object object) {
        return wrap(jcx.context(), jcx.scope(), object);
    }

    static Object wrap(Context context, Scriptable scope, Object object) {
        if (object == null || object instanceof Undefined) {
            return Undefined.instance;
        }

        if (object instanceof Wrapper) {
            object = ((Wrapper) object).unwrap();
        }

        if (object instanceof Scriptable) {
            return object;
        }

        // Simple types
        if (object instanceof String || object instanceof Integer
                || object instanceof Long || object instanceof Double || object instanceof Float
                || object instanceof Boolean) {
            return object;
        }

        if (object.getClass().isArray()) {
            return new NativeJavaArray(scope, object);
        }

        if (object instanceof ScriptingReference) {
            ScriptingReference ref = (ScriptingReference) object;
            return new Ref() {
                @Override
                public Object get(Context context) {
                    return ref.get(new JavaScriptContext(context, scope));
                }

                @Override
                public Object set(Context context, Object o) {
                    ref.set(new JavaScriptContext(context, scope), o);
                    return get(context);
                }
            };
        }

        // Exposed objects
        final Class<?> objectClass = object.getClass();
        final Exposed exposed = objectClass.getAnnotation(Exposed.class);
        if (exposed != null) {
            return new JavaScriptObject(object);
        }

        // Wrapper case -- go up in the hierarchy
        for (Map.Entry<Class, Constructor> entry : WRAPPERS.entrySet()) {
            if (entry.getKey().isAssignableFrom(objectClass)) {
                try {
                    return new JavaScriptObject(entry.getValue().newInstance(object));
                } catch (Exception e) {
                    throw new UnsupportedOperationException("Could not wrap object of class " + objectClass, e);
                }
            }
        }


//        return new NativeJavaObject(scope, object, Object.class);
        throw new IllegalArgumentException(format("Cannot wrap class %s into javascript object", objectClass));
    }

    @Override
    public void close() throws Exception {
        // Stop debugger
        if (debugger != null)
            try {
                debugger.stop();
            } catch (Exception e) {
                LOGGER.error(e);
            }

        // Close script context

        scriptContext.close();

        // Exit context
        Context.exit();
    }

    public Object evaluateReader(Reader reader, String filename, int lineno, Object security) throws Exception {
        ScriptContext.get().setCurrentScriptPath(Paths.get(filename));
        return context.evaluateReader(scope, reader, filename, lineno, security);
    }

    public Object evaluateString(String content, String filename, int lineno, Object security) throws Exception {
        return context.evaluateString(scope, content, filename, lineno, security);
    }

}
