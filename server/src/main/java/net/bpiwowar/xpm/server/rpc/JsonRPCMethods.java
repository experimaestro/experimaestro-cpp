package net.bpiwowar.xpm.server.rpc;

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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.gson.*;
import net.bpiwowar.xpm.connectors.LocalhostConnector;
import net.bpiwowar.xpm.exceptions.*;
import net.bpiwowar.xpm.manager.Repositories;
import net.bpiwowar.xpm.manager.js.JavaScriptRunner;
import net.bpiwowar.xpm.manager.python.PythonRunner;
import net.bpiwowar.xpm.scheduler.*;
import net.bpiwowar.xpm.utils.CloseableIterable;
import net.bpiwowar.xpm.utils.CloseableIterator;
import net.bpiwowar.xpm.utils.JSUtils;
import net.bpiwowar.xpm.utils.XPMInformation;
import net.bpiwowar.xpm.utils.log.DefaultFactory;
import net.bpiwowar.xpm.utils.log.Logger;
import net.bpiwowar.xpm.utils.log.Router;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.eclipse.jetty.server.Server;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptStackElement;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.python.core.PyException;

import javax.servlet.http.HttpServlet;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Json RPC methods
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@JsonRPCMethodsHolder("")
public class JsonRPCMethods extends HttpServlet {
    final static private Logger LOGGER = Logger.getLogger();

    private static Multimap<String, RPCCaller> methods;

    private final Scheduler scheduler;

    private final Repositories repository;

    private final JSONRPCRequest mos;

    HashMap<String, BufferedWriter> writers = new HashMap<>();

    HashSet<Listener> listeners = new HashSet<>();

    /**
     * Server
     */
    private Server server;
    private HashMap<Class<?>, Object> objects = new HashMap<>();

    public JsonRPCMethods(Server server, Scheduler scheduler, Repositories repository, JSONRPCRequest mos) throws IOException {
        initMethods();
        this.server = server;
        this.scheduler = scheduler;
        this.repository = repository;
        this.mos = mos;
        addObjects(this, new DocumentationMethods(), new ExperimentsMethods());
    }

    public void addObjects(Object... objects) {
        for (Object object : objects) {
            this.objects.put(object.getClass(), object);
        }
    }

    public static void initMethods() throws IOException {
        if (methods == null) {
            methods = HashMultimap.create();

            // Add methods from other classes
            addRPCMethods(JsonRPCMethods.class);
            addRPCMethods(DocumentationMethods.class);
            addRPCMethods(ExperimentsMethods.class);
        }
    }

    public static void addRPCMethods(Class<?> jsonRPCMethodsClass) {
        final JsonRPCMethodsHolder def = jsonRPCMethodsClass.getAnnotation(JsonRPCMethodsHolder.class);

        for (Method method : jsonRPCMethodsClass.getDeclaredMethods()) {
            final RPCMethod rpcMethod = method.getAnnotation(RPCMethod.class);
            if (rpcMethod != null) {
                String name = "".equals(rpcMethod.name()) ? method.getName() : rpcMethod.name();
                if (!def.value().isEmpty())
                    name = def.value() + "." + name;
                methods.put(name, new RPCMethodCaller(method));
            }
        }

        for (Class<?> aClass : jsonRPCMethodsClass.getDeclaredClasses()) {
            if (JsonCallable.class.isAssignableFrom(aClass)) {
                final RPCMethod rpcMethod = aClass.getAnnotation(RPCMethod.class);
                String name = rpcMethod == null || "".equals(rpcMethod.name()) ? aClass.getName() : rpcMethod.name();
                if (!def.value().isEmpty())
                    name = def.value() + "." + name;
                methods.put(name, new RPCClassCaller(aClass));
            }
        }
    }


    public void handle(String message) {
        try {
            JsonObject object;
            try {
                JsonParser parser = new JsonParser();
                object = parser.parse(message).getAsJsonObject();
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
        } finally {
            // Close DB connection if opened
            Scheduler.closeConnection();
        }
    }

    void handleJSON(JsonObject object) {
        String requestID = null;

        try {
            final JsonElement id = object.get("id");
            requestID = id == null ? null : id.getAsString();


            String command = object.get("method").getAsString();
            if (command == null)
                throw new RuntimeException("No method in JSON");

            final JsonObject p;
            if (!object.has("params")) {
                p = new JsonObject();
            } else {
                final JsonElement params = object.get("params");
                if (!params.isJsonObject()) {
                    throw new XPMCommandException("Parameters are not an object for %s", command);
                }
                p = params.getAsJsonObject();
            }


            Collection<RPCCaller> candidates = methods.get(command);
            int max = Integer.MIN_VALUE;
            RPCCaller argmax = null;

            candidateLoop:
            for (RPCCaller candidate : candidates) {
                int score = Integer.MAX_VALUE;
                for (Object _argument : candidate.arguments.values()) {
                    ArgumentDescriptor argument = (ArgumentDescriptor) _argument;
                    final boolean has = p.has(argument.name);
                    if (argument.required && !has) {
                        continue candidateLoop;
                    }
                    if (!has) --score;
                }

                if (score > max) {
                    max = score;
                    argmax = candidate;
                }
            }

            if (argmax == null)
                throw new XPMCommandException("Cannot find a matching method for " + command.toString());
            final Class<?> declaringClass = argmax.getDeclaringClass();

            Object result = argmax.call(objects.get(declaringClass), p);
            mos.endMessage(requestID, result);
        } catch (InvocationTargetException e) {
            try {
                Throwable t = e;
                while (t.getCause() != null) {
                    t = t.getCause();
                }

                // Handles an exit exception
                if (t instanceof ExitException) {
                    ExitException exitException = (ExitException) t;
                    if (exitException.getCode() != 0) {
                        try {
                            mos.error(requestID, exitException.getCode(), exitException.getMessage());
                            return;
                        } catch (IOException e2) {
                            LOGGER.error(e2, "Could not send the return code");
                        }
                    } else {
                        try {
                            mos.endMessage(requestID, null);
                            return;
                        } catch (IOException e1) {
                            LOGGER.error(e1, "Could not send the return code");
                        }
                    }
                }

                LOGGER.info(e, "Error while handling JSON request [%s]", e.toString());
                mos.error(requestID, 1, t.getMessage());
            } catch (IOException e2) {
                LOGGER.error(e2, "Could not send the return code");
            }
        } catch (XPMCommandException t) {
            try {
                mos.error(requestID, 1, "Error while running request: " + t.toString());
            } catch (IOException e) {
                LOGGER.error("Could not send the return code");
            }
        } catch (Throwable t) {
            LOGGER.info(t, "Internal error while handling JSON request");
            try {
                mos.error(requestID, 1, "Internal error while running request");
            } catch (IOException e) {
                LOGGER.error("Could not send the return code");
            }
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

    /**
     * Information about a job
     */
    @RPCMethod(help = "Returns detailed information about a job (Json format)")
    public JsonObject getResourceInformation(@RPCArgument(name = "id") String resourceId) throws IOException, SQLException {
        Resource resource = getResource(resourceId);

        if (resource == null)
            throw new XPMRuntimeException("No resource with id [%s]", resourceId);


        return resource.toJSON();
    }


    // -------- RPC METHODS -------

    /**
     * Get a resource by ID or by locator
     *
     * @param resourceId The resource ID or locator
     * @return
     */
    static private Resource getResource(String resourceId) throws SQLException {
        Resource resource;
        try {
            long rid = Long.parseLong(resourceId);
            resource = Resource.getById(rid);
        } catch (NumberFormatException e) {
            resource = Resource.getByLocator(resourceId);
        }

        return resource;
    }

    @RPCMethod(help = "Ping")
    public String ping() {
        return "pong";
    }

    @RPCMethod(help = "Sets a log level")
    public int setLogLevel(@RPCArgument(name = "identifier") String identifier, @RPCArgument(name = "level") String level) {
        final Logger logger = Logger.getLogger(identifier);
        logger.setLevel(Level.toLevel(level));
        return 0;
    }

    /**
     * Run javascript
     */
    @RPCMethod(name = "run-python", help = "Run a python file")
    public String runPython(@RPCArgument(name = "files") List<JsonArray> files,
                            @RPCArgument(name = "environment") Map<String, String> environment,
                            @RPCArgument(name = "debug", required = false) Integer debugPort,
                            @RPCArgument(name = "pythonpath", required = false) String pythonPath) {

        final StringWriter errString = new StringWriter();
//        final PrintWriter err = new PrintWriter(errString);

        final Hierarchy loggerRepository = getScriptLogger();

        // Enter JS context (so we have just one)
        Context.enter();
        try {
            // TODO: should be a one shot repository - ugly
            Repositories repositories = new Repositories(new File("/").toPath());
            repositories.add(repository, 0);

            // Creates and enters a Context. The Context stores information
            // about the execution environment of a script.
            try (PythonRunner pythonContext =
                         new PythonRunner(environment, repositories, scheduler, loggerRepository, pythonPath,
                                 getRequestOutputStream(), getRequestErrorStream())
            ) {
                Object result = null;
                for (JsonArray filePointer : files) {
                    boolean isFile = filePointer.size() < 2 || filePointer.get(1).isJsonNull();
                    final String content = isFile ? null : filePointer.get(1).getAsString();
                    final String filename = filePointer.get(0).getAsString();

                    final LocalhostConnector connector = Scheduler.get().getLocalhostConnector();
                    Path locator = connector.resolve(filename);
                    if (isFile) {
                        result = pythonContext.evaluateReader(connector, locator, new FileReader(filename), filename, 1, null);
                    } else {
                        result = pythonContext.evaluateString(connector, locator, content, filename, 1, null);
                    }
                }

                if (result != null)
                    LOGGER.debug("Returns %s", result.toString());
                else
                    LOGGER.debug("Null result");

                return result != null && result != Scriptable.NOT_FOUND &&
                        result != Undefined.instance ? result.toString() : "";

            } catch (ExitException e) {
                if (e.getCode() == 0) {
                    return null;
                }
                throw e;
            } catch (Throwable e) {
                // HACK: should not be necessary
                if (e instanceof PyException) {
                    try {
                        final Field printingStackTrace = PyException.class.getDeclaredField("printingStackTrace");
                        printingStackTrace.setAccessible(true);
                        printingStackTrace.set(e, true);
                        e.printStackTrace(System.err);
                        printingStackTrace.set(e, false);
                    } catch (Throwable e2) {
                    }
                }
                e.printStackTrace(System.err);

                Throwable wrapped = e;
                PyException pye = e instanceof PyException ? (PyException) e : null;
                LOGGER.info("Exception thrown there: %s", e.getStackTrace()[0]);

                while (wrapped.getCause() != null) {
                    wrapped = wrapped.getCause();
                    if (wrapped instanceof PyException) {
                        pye = (PyException) wrapped;
                    }
                }

                LOGGER.printException(Level.INFO, wrapped);

                org.apache.log4j.Logger logger = loggerRepository.getLogger("xpm-rpc");

                logger.error(wrapped.toString());

                for (Throwable ee = e; ee != null; ee = ee.getCause()) {
                    if (ee instanceof ContextualException) {
                        ContextualException ce = (ContextualException) ee;
                        List<String> context = ce.getContext();
                        if (!context.isEmpty()) {
                            logger.error("[Context]");
                            for (String s : context) {
                                logger.error(s);
                            }
                        }
                    }
                }

                if (pye != null) {
                    logger.error("[Stack trace]");
                    logger.error(pye.traceback.dumpStack());
                }

                throw new RuntimeException(errString.toString());

            }
        } finally {
            Context.exit();
        }
    }


    /**
     * Run javascript
     */
    @RPCMethod(name = "run-javascript", help = "Run a javascript")
    public String runJSScript(@RPCArgument(name = "files") List<JsonArray> files,
                              @RPCArgument(name = "environment") Map<String, String> environment,
                              @RPCArgument(name = "debug", required = false) Integer debugPort) {

        final StringWriter errString = new StringWriter();
//        final PrintWriter err = new PrintWriter(errString);

        final Hierarchy loggerRepository = getScriptLogger();

        // TODO: should be a one shot repository - ugly
        Repositories repositories = new Repositories(new File("/").toPath());
        repositories.add(repository, 0);

        Router.writer(getRequestErrorStream());

        // Creates and enters a Context. The Context stores information
        // about the execution environment of a script.
        try (JavaScriptRunner jsXPM = new JavaScriptRunner(repositories, scheduler, loggerRepository, debugPort)) {
            Object result = null;
            for (JsonArray filePointer : files) {
                boolean isFile = filePointer.size() < 2 || filePointer.get(1) == null;
                final String content = isFile ? null : filePointer.get(1).toString();
                final String filename = filePointer.get(0).toString();

                final LocalhostConnector connector = Scheduler.get().getLocalhostConnector();
                Path locator = connector.resolve(filename);
                if (isFile)
                    result = jsXPM.evaluateReader(new FileReader(filename), filename, 1, null);
                else
                    result = jsXPM.evaluateString(content, filename, 1, null);

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
            while (wrapped.getCause() != null) {
                wrapped = wrapped.getCause();
            }

            if (wrapped instanceof ExitException) {
                throw (ExitException) wrapped;
            }

            LOGGER.printException(Level.INFO, wrapped);

            org.apache.log4j.Logger logger = loggerRepository.getLogger("xpm-rpc");

            logger.error(wrapped.toString());

            for (Throwable ee = e; ee != null; ee = ee.getCause()) {
                if (ee instanceof ContextualException) {
                    ContextualException ce = (ContextualException) ee;
                    List<String> context = ce.getContext();
                    if (!context.isEmpty()) {
                        logger.error("[Context]");
                        for (String s : context) {
                            logger.error(s);
                        }
                    }
                }
            }

            if (wrapped instanceof NotImplementedException)
                logger.error(format("Line where the exception was thrown: %s", wrapped.getStackTrace()[0]));

            logger.error("[Stack trace]");
            final ScriptStackElement[] scriptStackTrace = JSUtils.getScriptStackTrace(wrapped);
            for (ScriptStackElement x : scriptStackTrace) {
                logger.error(format("  at %s:%d (%s)", x.fileName, x.lineNumber, x.functionName));
            }

            // TODO: We should have something better
//            if (wrapped instanceof RuntimeException && !(wrapped instanceof RhinoException)) {
//                err.format("Internal error:%n");
//                e.printStackTrace(err);
//            }

            throw new RuntimeException(errString.toString());

        }
    }


    private Hierarchy getScriptLogger() {
//        final RootLogger root = new RootLogger(Level.INFO);
        final Logger root = new Logger("root");
        root.setLevel(Level.INFO);
        final Hierarchy loggerRepository = new Hierarchy(root) {
            public Logger getLogger(String name) {
                return (Logger) this.getLogger(name, new DefaultFactory());
            }
        };
        BufferedWriter stringWriter = getRequestErrorStream();

        PatternLayout layout = new PatternLayout("%-6p [%c] %m%n");
        WriterAppender appender = new WriterAppender(layout, stringWriter);
        root.addAppender(appender);
        return loggerRepository;
    }

    /**
     * Return the output stream for the request
     */
    private BufferedWriter getRequestOutputStream() {
        return getRequestStream("out");
    }

    /**
     * Return the error stream for the request
     */
    private BufferedWriter getRequestErrorStream() {
        return getRequestStream("err");
    }

    /**
     * Return a stream with the given ID
     */
    private BufferedWriter getRequestStream(final String id) {
        BufferedWriter bufferedWriter = writers.get(id);
        if (bufferedWriter == null) {
            bufferedWriter = new BufferedWriter(new Writer() {
                @Override
                public void write(char[] cbuf, int off, int len) throws IOException {
                    ImmutableMap<String, String> map = ImmutableMap.of("stream", id, "value", new String(cbuf, off, len));
                    mos.message(map);
                }

                @Override
                public void flush() throws IOException {
                }

                @Override
                public void close() throws IOException {
                    throw new UnsupportedOperationException();
                }
            });
            writers.put(id, bufferedWriter);
        }
        return bufferedWriter;
    }

    /**
     * Shutdown the server
     */
    @RPCMethod(help = "Shutdown Experimaestro server")
    public boolean shutdown() {
        // Close the scheduler
        scheduler.close();

        // Shutdown jetty (after 1s to allow this thread to finish)
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                boolean stopped = false;
                try {
                    server.stop();
                    stopped = true;
                } catch (Exception e) {
                    LOGGER.error(e, "Could not stop properly jetty");
                }
                if (!stopped)
                    synchronized (this) {
                        try {
                            wait(10000);
                        } catch (InterruptedException e) {
                            LOGGER.error(e);
                        }
                        System.exit(1);

                    }
            }
        }, 2000);

        // Everything's OK
        return true;
    }

    // Restart all the job (recursion)
    static private int invalidate(Resource resource) throws Exception {
        int nbUpdated = 0;
        try (final CloseableIterator<Dependency> deps = resource.getOutgoingDependencies(false)) {

            while (deps.hasNext()) {
                Dependency dependency = deps.next();
                final Resource to = dependency.getTo();
                LOGGER.info("Invalidating %s", to);

                invalidate(to);

                final ResourceState state = to.getState();
                if (state == ResourceState.RUNNING)
                    ((Job) to).stop();
                if (!state.isActive()) {
                    nbUpdated++;
                    // We invalidate grand-children if the child was done
                    if (state == ResourceState.DONE) {
                        invalidate(to);
                    }
                    ((Job) to).restart();
                }
            }
        }
        return nbUpdated;
    }

    @RPCMethod(name = "restart", help = "Puts back a job into the waiting queue")
    static public class Restart implements JsonCallable {
        @RPCArgument(name = "id", help = "The id of the job (string or integer)")
        String id;

        @RPCArgument(name = "restart-done", help = "Whether done jobs should be invalidated")
        boolean restartDone;

        @RPCArgument(name = "recursive", help = "Whether we should invalidate dependent results when the job was done")
        boolean recursive;

        @Override
        public Object call() throws Throwable {
            int nbUpdated = 0;
            Resource resource = getResource(id);
            if (resource == null)
                throw new XPMRuntimeException("Job not found [%s]", id);

            final ResourceState rsrcState = resource.getState();

            if (rsrcState == ResourceState.RUNNING)
                throw new XPMRuntimeException("Job is running [%s]", rsrcState);

            // The job is active, so we have nothing to do
            if (rsrcState.isActive())
                return 0;

            if (!restartDone && rsrcState == ResourceState.DONE)
                return 0;

            ((Job) resource).restart();
            nbUpdated++;

            // If the job was done, we need to restart the dependences
            if (recursive && rsrcState == ResourceState.DONE) {
                nbUpdated += invalidate(resource);
            }

            return nbUpdated;
        }
    }

    /**
     * Update the status of jobs
     */
    @RPCMethod(help = "Force the update of all the jobs statuses. Returns the number of jobs whose update resulted" +
            " in a change of state")
    public int updateJobs(
            @RPCArgument(name = "recursive", required = false) Boolean _recursive,
            @RPCArgument(name = "states", required = false) String[] statesNames
    ) throws Exception {
        EnumSet<ResourceState> states = getStates(statesNames);

        int nbUpdated = 0;
        try (final CloseableIterable<Resource> resources = scheduler.resources(states)) {
            for (Resource resource : resources) {
                if (resource.updateStatus()) {
                    nbUpdated++;
                } else {
                }
            }
        } catch (CloseException e) {
            throw new RuntimeException(e);
        }
        // Just in case
        Scheduler.notifyRunners();

        return nbUpdated;

    }

    /**
     * Remove resources specified with the given filter
     *
     * @param id          The URI of the resource to delete
     * @param statesNames The states of the resource to delete
     */
    @RPCMethod(name = "remove", help = "Remove jobs")
    public int remove(@RPCArgument(name = "id", required = false) String id,
                      @RPCArgument(name = "regexp", required = false) Boolean _idIsRegexp,
                      @RPCArgument(name = "states", required = false) String[] statesNames,
                      @RPCArgument(name = "recursive", required = false) Boolean _recursive
    ) throws Exception {
        int n = 0;
        EnumSet<ResourceState> states = getStates(statesNames);
        boolean recursive = _recursive != null ? _recursive : false;

        Pattern idPattern = _idIsRegexp != null && _idIsRegexp ?
                Pattern.compile(id) : null;

        if (id != null && !id.equals("") && idPattern == null) {
            final Resource resource = getResource(id);
            if (resource == null)
                throw new XPMCommandException("Job not found [%s]", id);


            if (!states.contains(resource.getState()))
                throw new XPMCommandException("Resource [%s] state [%s] not in [%s]",
                        resource, resource.getState(), states);
            resource.delete(recursive);
            n = 1;
        } else {
            // TODO order the tasks so that dependencies are removed first
            HashSet<Resource> toRemove = new HashSet<>();
            try (final CloseableIterable<Resource> resources = scheduler.resources(states)) {
                for (Resource resource : resources) {
                    if (idPattern != null) {
                        if (!idPattern.matcher(resource.getIdentifier()).matches())
                            continue;
                    }
                    try {
                        toRemove.add(resource);
                    } catch (Exception e) {
                        // TODO should output this to the caller
                    }
                    n++;
                }
            }

            for (Resource resource : toRemove)
                resource.delete(recursive);

        }
        return n;
    }

    @RPCMethod(help = "Listen to XPM events")
    public boolean listen() {
        Listener listener = message -> {
            try {
                HashMap<String, Object> map = new HashMap<>();
                map.put("event", message.getType().toString());

                if (message instanceof SimpleMessage) {
                    final SimpleMessage simpleMessage = (SimpleMessage) message;
                    final Resource resource = simpleMessage.getResource();
                    final Long resourceID = simpleMessage.getResourceID();
                    if (resourceID == null) {
                        return;
                    }

                    map.put("id", resourceID);
                    Path locator = resource.getLocator();
                    if (locator != null)
                        map.put("locator", locator.toString());

                    switch (message.getType()) {
                        case STATE_CHANGED:
                            map.put("state", resource.getState().toString());
                            break;

                        case PROGRESS:
                            map.put("progress", ((Job) resource).getProgress());
                        case RESOURCE_REMOVED:
                            break;

                        case RESOURCE_ADDED:
                            map.put("state", resource.getState().toString());
                            break;
                    }

                    mos.message(map);
                }

            } catch (IOException e) {
                LOGGER.error(e, "Could not output");
            } catch (RuntimeException e) {
                LOGGER.error(e, "Error while trying to notify RPC client");
            }
        };

        listeners.add(listener);
        scheduler.addListener(listener);
        return true;
    }

    public void close() {
        for (Listener listener : listeners)
            scheduler.removeListener(listener);
    }

    /**
     * Kills all the jobs in a group
     */
    @RPCMethod(help = "Stops a set of jobs under a given group.")
    public int stopJobs(
            @RPCArgument(name = "killRunning", help = "Whether to kill running tasks") boolean killRunning,
            @RPCArgument(name = "holdWaiting", help = "Whether to put on hold ready/waiting tasks") boolean holdWaiting,
            @RPCArgument(name = "recursive", help = "Recursive") boolean recursive) throws Exception {

        final EnumSet<ResourceState> statesSet
                = EnumSet.of(ResourceState.RUNNING, ResourceState.READY, ResourceState.WAITING);

        int n = 0;
        try (final CloseableIterable<Resource> resources = scheduler.resources(statesSet)) {
            for (Resource resource : resources) {
                if (resource instanceof Job) {
                    ((Job) resource).stop();
                    n++;
                }
            }
        } catch (CloseException e) {
            throw new RuntimeException(e);
        }
        return n;
    }

    @RPCMethod(help = "Generate files for starting associated process")
    public void generateFiles(@RPCArgument(name = "jobs", required = true) String[] JobIds) {
        final Logger logger = (Logger) getScriptLogger().getLogger("rpc");
        for (String id : JobIds) {
            try {
                final Resource resource = getResource(id);
                if (resource instanceof Job) {
                    ((Job) resource).generateFiles();
                }
            } catch (Throwable throwable) {
                logger.error(throwable, "Could not generate files for resource [%s]", id);
            }
        }
    }

    @RPCMethod(help = "Kill one or more jobs")
    public int kill(@RPCArgument(name = "jobs", required = true) String[] JobIds) {
        final Logger logger = (Logger) getScriptLogger().getLogger("rpc");
        int n = 0;
        for (String id : JobIds) {
            try {
                final Resource resource = getResource(id);
                if (resource instanceof Job) {
                    if (((Job) resource).stop()) {
                        n++;
                    }
                }
            } catch (Throwable throwable) {
                logger.error("Error while killing jbo [%s]", id);
            }
        }
        return n;
    }

    /**
     * List jobs
     */
    @RPCMethod(help = "List the jobs along with their states")
    public List<Map<String, String>> listJobs(
            @RPCArgument(name = "states") String[] states,
            @RPCArgument(name = "recursive", required = false) Boolean _recursive) {
        final EnumSet<ResourceState> set = getStates(states);
        List<Map<String, String>> list = new ArrayList<>();
        boolean recursive = _recursive == null ? false : _recursive;

        try (final CloseableIterable<Resource> resources = scheduler.resources(set)) {
            for (Resource resource : resources) {
                Map<String, String> map = getResourceJSON(resource);
                list.add(map);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public static Map<String, String> getResourceJSON(Resource resource) {
        Map<String, String> map = new HashMap<>();
        map.put("type", resource.getClass().getCanonicalName());
        map.put("state", resource.getState().toString());
        map.put("name", resource.getLocator().toString());
        map.put("id", resource.getId().toString());
        return map;
    }

    /**
     * Get information about experimaestro
     */
    @RPCMethod(help = "Get build information about experimaestro")
    public XPMInformation buildInformation() {
        return XPMInformation.get();
    }

    /**
     * Utility function that transforms an array with paired values into a map
     *
     * @param envArray The array, must contain an even number of elements
     * @return a map
     */
    private Map<String, String> arrayToMap(Object[] envArray) {
        Map<String, String> env = new TreeMap<>();
        for (Object x : envArray) {
            Object[] o = (Object[]) x;
            if (o.length != 2)
                // FIXME: should be a proper one
                throw new RuntimeException();
            env.put((String) o[0], (String) o[1]);
        }
        return env;
    }


    public static class ArgumentDescriptor {
        String name;
        boolean required;

        ArgumentDescriptor(RPCArgument annotation) {
            this.required = annotation.required();
            this.name = annotation.name();
        }
    }

    public abstract static class RPCCaller<T extends ArgumentDescriptor> {
        public abstract Object call(Object o, JsonObject p) throws Throwable;

        public abstract Class<?> getDeclaringClass();

        /**
         * Arguments
         */
        HashMap<String, T> arguments = new HashMap<>();

    }

    public static class MethodArgumentDescriptor extends ArgumentDescriptor {
        int position;

        public MethodArgumentDescriptor(RPCArgument annotation, int position) {
            super(annotation);
            this.position = position;
        }
    }

    static public class RPCMethodCaller extends RPCCaller<MethodArgumentDescriptor> {
        /**
         * Method
         */
        Method method;


        public RPCMethodCaller(Method method) {
            this.method = method;
            final Type[] types = method.getGenericParameterTypes();
            Annotation[][] annotations = method.getParameterAnnotations();
            mainLoop:
            for (int i = 0; i < annotations.length; i++) {
                for (int j = 0; j < annotations[i].length; j++) {
                    if (annotations[i][j] instanceof RPCArgument) {
                        final RPCArgument annotation = (RPCArgument) annotations[i][j];
                        final String name = annotation.name();
                        arguments.put(name, new MethodArgumentDescriptor(annotation, i));
                        continue mainLoop;
                    }
                }

                throw new XPMRuntimeException("No annotation for %dth argument of %s", i + 1, method);
            }
        }

        @Override
        public Object call(Object o, JsonObject p) throws InvocationTargetException, IllegalAccessException {
            Object[] args = new Object[method.getParameterCount()];
            Gson gson = new Gson();
            final Type[] types = method.getGenericParameterTypes();
            int index = 0;
            for (MethodArgumentDescriptor descriptor : arguments.values()) {
                final JsonElement jsonElement = p.get(descriptor.name);
                final Type type = types[index];
                try {
                    args[index] = gson.fromJson(jsonElement, type);
                } catch (RuntimeException e) {
                    throw new XPMCommandException(e).addContext("while processing parameter %s", descriptor.name);
                }
                ++index;
            }

            return method.invoke(o, args);
        }

        @Override
        public Class<?> getDeclaringClass() {
            return method.getDeclaringClass();
        }
    }


    public static class ClassArgumentDescriptor extends ArgumentDescriptor {

        private final Field field;

        public ClassArgumentDescriptor(RPCArgument annotation, Field field) {
            super(annotation);
            final String name = annotation.name();
            this.name = name.isEmpty() ? field.getName() : name;
            this.field = field;
        }
    }

    private static class RPCClassCaller extends RPCCaller<ClassArgumentDescriptor> {
        private final Class<?> rpcClass;

        public RPCClassCaller(Class<?> rpcClass) {
            this.rpcClass = rpcClass;
            if (!JsonCallable.class.isAssignableFrom(rpcClass)) {
                throw new AssertionError("An RPC method class should implement the JsonCallable interface");
            }

            for (Field field : rpcClass.getDeclaredFields()) {
                final RPCArgument annotation = field.getAnnotation(RPCArgument.class);
                if (!Modifier.isStatic(rpcClass.getModifiers()))
                    throw new AssertionError("An RPC method class should be static");
                if (annotation != null) {
                    final ClassArgumentDescriptor descriptor = new ClassArgumentDescriptor(annotation, field);
                    arguments.put(descriptor.name, descriptor);
                }
            }
        }

        @Override
        public Object call(Object o, JsonObject p) throws Throwable {
            Gson gson = new Gson();
            final JsonCallable object = (JsonCallable) rpcClass.newInstance();
            for (ClassArgumentDescriptor descriptor : arguments.values()) {
                final JsonElement jsonElement = p.get(descriptor.name);
                final Type type = descriptor.field.getGenericType();
                try {
                    Object value = gson.fromJson(jsonElement, type);
                    if (!descriptor.field.isAccessible()) {
                        descriptor.field.setAccessible(true);
                        descriptor.field.set(object, value);
                        descriptor.field.setAccessible(false);
                    } else {
                        descriptor.field.set(object, value);
                    }
                } catch (RuntimeException e) {
                    throw new XPMCommandException(e).addContext("while processing parameter %s", descriptor.name);
                }
            }

            return object.call();
        }

        @Override
        public Class<?> getDeclaringClass() {
            return rpcClass;
        }
    }
}
