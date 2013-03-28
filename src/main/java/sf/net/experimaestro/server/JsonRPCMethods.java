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

package sf.net.experimaestro.server;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.RootLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.exceptions.ContextualException;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.Repositories;
import sf.net.experimaestro.manager.js.XPMObject;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.ResourceLocator;
import sf.net.experimaestro.scheduler.ResourceState;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.Cleaner;
import sf.net.experimaestro.utils.log.Logger;

import javax.servlet.http.HttpServlet;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 28/3/13
 */
public class JsonRPCMethods extends HttpServlet {
    final static private Logger LOGGER = Logger.getLogger();
    private final Scheduler scheduler;
    private final Repositories repository;
    private final JSONRPCRequest mos;

    public JsonRPCMethods(Scheduler scheduler, Repositories repository, JSONRPCRequest mos) {
        this.scheduler = scheduler;
        this.repository = repository;
        this.mos = mos;
    }


    static public class MethodDescription {
        Method method;
        RPCArgument arguments[];
        Class<?>[] types;

        public MethodDescription(Method method) {
            this.method = method;
            types = method.getParameterTypes();
            Annotation[][] annotations = method.getParameterAnnotations();
            arguments = new RPCArgument[annotations.length];
            for (int i = 0; i < annotations.length; i++) {
                types[i] = ClassUtils.primitiveToWrapper(types[i]);
                for (int j = 0; j < annotations[i].length && arguments[i] == null; j++) {
                    if (annotations[i][j] instanceof RPCArgument)
                        arguments[i] = (RPCArgument) annotations[i][j];
                }

                if (arguments[i] == null)
                    throw new ExperimaestroRuntimeException("No annotation for %dth argument of %s", i + 1, method);

            }
        }
    }

    private static Multimap<String, MethodDescription> methods = HashMultimap.create();

    static {
        for (Method method : JsonRPCMethods.class.getDeclaredMethods()) {
            final RPCMethod rpcMethod = method.getAnnotation(RPCMethod.class);
            if (rpcMethod != null) {
                methods.put("".equals(rpcMethod.name()) ? method.getName() : rpcMethod.name(), new MethodDescription(method));
            }
        }

    }

    private int convert(Object p, MethodDescription description, int score, Object args[], int index) {
        Object o;
        if (p instanceof JSONObject)
            // If p is a map, then use the json name of the argument
            o = ((JSONObject) p).get(description.arguments[index].name());
        else if (p instanceof JSONArray)
            // if it is an array, then map it
            o = ((JSONArray) p).get(index);
        else {
            // otherwise, suppose it is a one value array
            if (index > 0)
                return Integer.MIN_VALUE;
            o = p;
        }

        Class aType = description.types[index];

        if (o == null) {
            if (description.arguments[index].required())
                return Integer.MIN_VALUE;

            return score - 10;
        }

        if (aType.isAssignableFrom(o.getClass())) {
            if (args != null)
                args[index] = o;
            return score;
        }
        return Integer.MIN_VALUE;
    }


    public void handle(String message) {
        JSONObject object;
        try {
            Object parse = JSONValue.parse(message);
            object = (JSONObject) parse;

        } catch (Throwable t) {
            LOGGER.warn(t, "Error while handling JSON request");

            try {
                mos.error(null, 1, "Could not parse JSON: " + t.getMessage());
            } catch (IOException e) {
                LOGGER.error(e, "Could not send the error message");
            }
            return;
        }

        handleJSON(object);
    }

    void handleJSON(JSONObject object) {
        String requestID = null;

        try {
            requestID = object.get("id").toString();
            if (requestID == null)
                throw new RuntimeException("No id in JSON request");


            Object command = object.get("method");
            if (command == null)
                throw new RuntimeException("No method in JSON");

            if (!object.containsKey("params"))
                throw new RuntimeException("No params in JSON");
            Object p = object.get("params");

            Collection<MethodDescription> candidates = methods.get(command.toString());
            int max = Integer.MIN_VALUE;
            MethodDescription argmax = null;
            for (MethodDescription candidate : candidates) {
                int score = Integer.MAX_VALUE;
                for (int i = 0; i < candidate.types.length && score > max; i++) {
                    score = convert(p, candidate, score, null, i);
                }
                if (score > max) {
                    max = score;
                    argmax = candidate;
                }
            }

            if (argmax == null)
                throw new ExperimaestroRuntimeException("Cannot find a matching method");

            Object[] args = new Object[argmax.arguments.length];
            for (int i = 0; i < args.length; i++) {
                int score = convert(p, argmax, 0, args, i);
                assert score > Integer.MIN_VALUE;
            }
            Object result = argmax.method.invoke(this, args);
            mos.endMessage(requestID, result);
        } catch (Throwable t) {
            LOGGER.error(t, "Error while handling JSON request");
            try {
                while (t.getCause() != null)
                    t = t.getCause();
                mos.error(requestID, 1, t.getMessage());
            } catch (IOException e) {
                LOGGER.error("Could not send the return code");
            }
            return;
        }
    }


    /**
     * Information about a job
     */
    @RPCMethod(help = "Returns detailed information about a job (XML format)")
    public JSONObject getResourceInformation(@RPCArgument(name = "id") String resourceId) throws IOException {
        Resource resource;
        try {
            long rid = Long.parseLong(resourceId);
            resource = scheduler.getResource(rid);
        } catch (NumberFormatException e) {
            resource = scheduler.getResource(ResourceLocator.parse(resourceId));
        }

        if (resource == null)
            throw new ExperimaestroRuntimeException("No resource with id [%s]", resourceId);


        return resource.toJSON();
    }

    /**
     * Run javascript
     *
     * @param files
     * @param environment
     */
    @RPCMethod(name = "run-javascript", help = "Run a javascript")
    public String runJSScript(@RPCArgument(name = "files") List<JSONArray> files,
                              @RPCArgument(name = "environment") Map<String, String> environment) {
        final StringWriter errString = new StringWriter();
        final PrintWriter err = new PrintWriter(errString);
        XPMObject jsXPM;

        final RootLogger root = new RootLogger(Level.INFO);
        final Hierarchy loggerRepository = new Hierarchy(root);
        BufferedWriter stringWriter = new BufferedWriter(new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                mos.message(new String(cbuf, off, len));
            }

            @Override
            public void flush() throws IOException {
            }

            @Override
            public void close() throws IOException {
                throw new UnsupportedOperationException();
            }
        });

        PatternLayout layout = new PatternLayout("%-6p [%c] %m%n");
        WriterAppender appender = new WriterAppender(layout, stringWriter);
        root.addAppender(appender);

        // Creates and enters a Context. The Context stores information
        // about the execution environment of a script.
        try (Cleaner cleaner = new Cleaner()) {
            Context jsContext = Context
                    .enter();

            // Initialize the standard objects (Object, Function, etc.)
            // This must be done before scripts can be executed. Returns
            // a scope object that we use in later calls.
            Scriptable scope = jsContext.initStandardObjects();


            // TODO: should be a one shot repository - ugly
            Repositories repositories = new Repositories(new ResourceLocator(LocalhostConnector.getInstance(), ""));
            repositories.add(repository, 0);

            ScriptableObject.defineProperty(scope, "env", new RPCHandler.JSGetEnv(environment), 0);
            jsXPM = new XPMObject(null, jsContext, environment, scope, repositories,
                    scheduler, loggerRepository, cleaner);

            Object result = null;
            for (int i = 0; i < files.size(); i++) {
                JSONArray filePointer = files.get(i);

                boolean isFile = filePointer.size() < 2 || filePointer.get(1) == null;
                final String content = isFile ? null : filePointer.get(1).toString();
                final String filename = filePointer.get(0).toString();

                ResourceLocator locator = new ResourceLocator(LocalhostConnector.getInstance(), isFile ? filename : "/");
                jsXPM.setLocator(locator);
                LOGGER.info("Script locator is %s", locator);

                if (isFile)
                    result = jsContext.evaluateReader(scope, new FileReader(filename), filename, 1, null);
                else
                    result = jsContext.evaluateString(scope, content, filename, 1, null);

            }

            if (result != null)
                LOGGER.debug("Returns %s", result.toString());
            else
                LOGGER.debug("Null result");

            return result != null && result != Scriptable.NOT_FOUND &&
                    result != Undefined.instance ? result.toString() : "";

        } catch (Throwable e) {
            Throwable wrapped = e;
            LOGGER.info("Exception thrown there: %s", e.getStackTrace()[0]);
            while (wrapped.getCause() != null)
                wrapped = wrapped.getCause();

            LOGGER.printException(Level.INFO, wrapped);

            err.println(wrapped.toString());

            for (Throwable ee = e; ee != null; ee = ee.getCause()) {
                if (ee instanceof ContextualException) {
                    ContextualException ce = (ContextualException) ee;
                    List<String> context = ce.getContext();
                    if (!context.isEmpty()) {
                        err.format("%n[context]%n");
                        for (String s : ce.getContext()) {
                            err.format("%s%n", s);
                        }
                    }
                }
            }

            if (wrapped instanceof NotImplementedException)
                err.format("Line where the exception was thrown: %s", wrapped.getStackTrace()[0]);

            // Search for innermost rhino exception
            RhinoException rhinoException = null;
            for (Throwable t = e; t != null; t = t.getCause())
                if (t instanceof RhinoException)
                    rhinoException = (RhinoException) t;

            if (rhinoException != null) {
                err.append("\n" + rhinoException.getScriptStackTrace());
            } else {
                err.format("Internal error:%n");
                e.printStackTrace(err);
            }

            err.flush();
            throw new RuntimeException(errString.toString());

        } finally {
            // Exit context
            Context.exit();
        }
    }


    private EnumSet<ResourceState> getStates(Object[] states) {
        final EnumSet<ResourceState> statesSet;

        if (states == null || states.length == 0)
            statesSet = EnumSet.allOf(ResourceState.class);
        else {
            ResourceState statesArray[] = new ResourceState[states.length];
            for (int i = 0; i < states.length; i++)
                statesArray[i] = ResourceState.valueOf(states[i].toString());
            statesSet = EnumSet.of(statesArray[0], statesArray);
        }
        return statesSet;
    }

}
