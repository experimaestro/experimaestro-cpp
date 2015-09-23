package sf.net.experimaestro.server;

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
import com.google.gson.GsonBuilder;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.eclipse.jetty.server.Server;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptStackElement;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.python.core.PyException;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.exceptions.*;
import sf.net.experimaestro.manager.Repositories;
import sf.net.experimaestro.manager.js.JavaScriptRunner;
import sf.net.experimaestro.manager.python.PythonRunner;
import sf.net.experimaestro.scheduler.*;
import sf.net.experimaestro.utils.CloseableIterable;
import sf.net.experimaestro.utils.CloseableIterator;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.XPMInformation;
import sf.net.experimaestro.utils.log.DefaultFactory;
import sf.net.experimaestro.utils.log.Logger;
import sf.net.experimaestro.utils.log.Router;

import javax.servlet.http.HttpServlet;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
public class JsonRPCMethods extends HttpServlet {
    final static private Logger LOGGER = Logger.getLogger();

    private static Multimap<String, MethodDescription> methods;

    static {
        initMethods();
    }

    private final Scheduler scheduler;

    private final Repositories repository;

    private final JSONRPCRequest mos;

    HashMap<String, BufferedWriter> writers = new HashMap<>();

    HashSet<Listener> listeners = new HashSet<>();

    /**
     * Server
     */
    private Server server;

    public JsonRPCMethods(Server server, Scheduler scheduler, Repositories repository, JSONRPCRequest mos) {
        this.server = server;
        this.scheduler = scheduler;
        this.repository = repository;
        this.mos = mos;
    }

    public static void initMethods() {
        if (methods == null) {
            methods = HashMultimap.create();
            for (Method method : JsonRPCMethods.class.getDeclaredMethods()) {
                final RPCMethod rpcMethod = method.getAnnotation(RPCMethod.class);
                if (rpcMethod != null) {
                    methods.put("".equals(rpcMethod.name()) ? method.getName() : rpcMethod.name(), new MethodDescription(method));
                }
            }
        }
    }

    private int convert(Object p, Arguments description, int score, List args, int index) {
        Object o;
        if (p instanceof JSONObject)
            // If p is a map, then use the json name of the argument
            o = ((JSONObject) p).get(description.getArgument(index).name());
        else if (p instanceof JSONArray)
            // if it is an array, then map it
            o = ((JSONArray) p).get(index);
        else {
            // otherwise, suppose it is a one value array
            if (index > 0)
                return Integer.MIN_VALUE;
            o = p;
        }

        final Class aType = description.getType(index);

        if (o == null) {
            if (description.getArgument(index).required())
                return Integer.MIN_VALUE;

            return score - 10;
        }

        if (aType.isArray()) {
            if (o instanceof JSONArray) {
                final JSONArray array = (JSONArray) o;


                final ArrayList arrayObjects;
                if (args != null) {
                    arrayObjects = new ArrayList(array.size());
                    for (int i = 0; i < array.size(); i++) {
                        arrayObjects.add(null);
                    }
                } else {
                    arrayObjects = null;
                }


                Arguments arguments = new Arguments() {
                    @Override
                    public RPCArgument getArgument(int i) {
                        return new RPCArrayArgument();
                    }

                    @Override
                    public Class<?> getType(int i) {
                        return aType.getComponentType();
                    }

                    @Override
                    public int size() {
                        return array.size();
                    }
                };


                for (int i = 0; i < array.size() && score > Integer.MIN_VALUE; i++) {
                    score = convert(array.get(i), arguments, score, arrayObjects, i);
                }

                if (args != null && score > Integer.MIN_VALUE) {
                    final Object a1 = Array.newInstance(aType.getComponentType(), array.size());
                    for (int i = 0; i < array.size(); i++) {
                        Array.set(a1, i, arrayObjects.get(i));
                    }
                    args.set(index, a1);
                }
                return score;
            }
            return Integer.MIN_VALUE;
        }

        if (aType.isAssignableFrom(o.getClass())) {
            if (args != null)
                args.set(index, o);
            return score;
        }

        if (o.getClass() == Long.class && aType == Integer.class) {
            if (args != null)
                args.set(index, ((Long) o).intValue());
            return score - 1;
        }

        return Integer.MIN_VALUE;
    }

    public void handle(String message) {
        try {
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
        } finally {
            // Close DB connection if opened
            Scheduler.closeConnection();
        }
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
                throw new XPMCommandException("Cannot find a matching method");

            Object[] args = new Object[argmax.arguments.length];
            for (int i = 0; i < args.length; i++) {
                int score = convert(p, argmax, 0, Arrays.asList(args), i);
                assert score > Integer.MIN_VALUE;
            }
            Object result = argmax.method.invoke(this, args);
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
    public JSONObject getResourceInformation(@RPCArgument(name = "id") String resourceId) throws IOException, SQLException {
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
    private Resource getResource(String resourceId) throws SQLException {
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
    public String runPython(@RPCArgument(name = "files") List<JSONArray> files,
                            @RPCArgument(name = "environment") Map<String, String> environment,
                            @RPCArgument(name = "debug", required = false) Integer debugPort) {

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
                         new PythonRunner(environment, repositories, scheduler, loggerRepository,
                                 getRequestOutputStream(), getRequestErrorStream())
            ) {
                Object result = null;
                for (JSONArray filePointer : files) {
                    boolean isFile = filePointer.size() < 2 || filePointer.get(1) == null;
                    final String content = isFile ? null : filePointer.get(1).toString();
                    final String filename = filePointer.get(0).toString();

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
    public String runJSScript(@RPCArgument(name = "files") List<JSONArray> files,
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
            for (JSONArray filePointer : files) {
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
    private int invalidate(Resource resource) throws Exception {
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

    @RPCMethod(help = "Puts back a job into the waiting queue")
    public int restart(
            @RPCArgument(name = "id", help = "The id of the job (string or integer)") String id,
            @RPCArgument(name = "restart-done", help = "Whether done jobs should be invalidated") boolean restartDone,
            @RPCArgument(name = "recursive", help = "Whether we should invalidate dependent results when the job was done") boolean recursive
    ) throws Exception {
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
    public void listen() {
        Listener listener = message -> {
            try {
                HashMap<String, Object> map = new HashMap<>();
                map.put("event", message.getType().toString());
                final Resource resource = ((SimpleMessage) message).getResource();
                if (message instanceof SimpleMessage) {
                    map.put("resource", resource.getId());
                    Path locator = resource.getLocator();
                    if (locator != null)
                        map.put("locator", locator.toString());
                }

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
            } catch (IOException e) {
                LOGGER.error(e, "Could not output");
            } catch (RuntimeException e) {
                LOGGER.error(e, "Error while trying to notify RPC client");
            }
        };

        listeners.add(listener);
        scheduler.addListener(listener);
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
                logger.error("Could not retrieve resource [%s]", id);
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

    @RPCMethod(help = "Puts back a job into the waiting queue")
    public int restartJob(
            @RPCArgument(name = "name", help = "The ID of the job") String name,
            @RPCArgument(name = "restart-done", help = "Whether done jobs should be invalidated") boolean restartDone,
            @RPCArgument(name = "recursive", help = "Whether we should invalidate dependent results when the job was done") boolean recursive
    ) throws Exception {
        int nbUpdated = 0;

        ResourceState rsrcState;
        Resource resource;
        resource = Resource.getByLocator(name);

        if (resource == null)
            throw new XPMRuntimeException("Job not found [%s]", name);

        rsrcState = resource.getState();

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

    /**
     * List jobs
     */
    @RPCMethod(help = "List the jobs along with their states")
    public List<Map<String, String>> listJobs(
            @RPCArgument(name = "group") String group,
            @RPCArgument(name = "states") String[] states,
            @RPCArgument(name = "recursive", required = false) Boolean _recursive) {
        final EnumSet<ResourceState> set = getStates(states);
        List<Map<String, String>> list = new ArrayList<>();
        boolean recursive = _recursive == null ? false : _recursive;

        try (final CloseableIterable<Resource> resources = scheduler.resources(set)) {
            for (Resource resource : resources) {
                Map<String, String> map = new HashMap<>();
                map.put("type", resource.getClass().getCanonicalName());
                map.put("state", resource.getState().toString());
                map.put("name", resource.getLocator().toString());
                list.add(map);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    /**
     * Get information about experimaestro
     */
    @RPCMethod(help = "Get build information about experimaestro")
    public Object buildInformation() {
        return JSONValue.parse(new GsonBuilder().create().toJson(XPMInformation.get()));
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


    public interface Arguments {
        RPCArgument getArgument(int i);

        Class<?> getType(int i);

        int size();
    }


//	/**
//	 * Add a data resource
//	 *
//	 * @param id
//	 *            The data ID
//	 * @param mode
//	 *            The locking mode
//	 * @param exists
//	 * @return
//	 * @throws DatabaseException
//	 */
//	public boolean addData(String id, String mode, boolean exists)
//			throws DatabaseException {
//		LOGGER.info("Addind data %s [%s/%b]", id, mode, exists);
//
//
//        ResourceLocator identifier = ResourceLocator.decode(id);
//		scheduler.append(new SimpleData(scheduler, identifier, LockMode.valueOf(mode),
//				exists));
//		return true;
//	}

    static public class MethodDescription implements Arguments {
        Method method;

        private RPCArgument[] arguments;

        private Class<?>[] types;

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
                    throw new XPMRuntimeException("No annotation for %dth argument of %s", i + 1, method);

            }
        }

        @Override
        public RPCArgument getArgument(int i) {
            return arguments[i];
        }

        @Override
        public Class<?> getType(int i) {
            return types[i];
        }

        @Override
        public int size() {
            return arguments.length;
        }
    }


//    /**
//     * Add a command line job
//     *
//     * @throws DatabaseException
//     */
//    public boolean runCommand(String name, int priority, Object[] command,
//                              Object[] envArray, String workingDirectory, Object[] depends,
//                              Object[] readLocks, Object[] writeLocks) throws DatabaseException, ExperimaestroCannotOverwrite {
//        Map<String, String> env = arrayToMap(envArray);
//        LOGGER.info(
//                "Running command %s [%s] (priority %d); read=%s, write=%s; environment={%s}",
//                name, Arrays.toString(command), priority,
//                Arrays.toString(readLocks), Arrays.toString(writeLocks),
//                Output.toString(", ", env.entrySet()));
//
//        CommandArguments commandArgs = new CommandArguments();
//        for (int i = command.length; --i >= 0; )
//            commandArgs.add(new CommandArgument(command[i].toString()));
//
//        Connector connector = Scheduler.get().getLocalhostConnector();
//        CommandLineTask job = new CommandLineTask(scheduler, connector, name, commandArgs,
//                env, new File(workingDirectory).getAbsolutePath());
//
//        // XPMProcess locks
//        for (Object depend : depends) {
//
//            Resource resource = scheduler.getResource(toResourceLocator(depend));
//            if (resource == null)
//                throw new RuntimeException("Resource " + depend
//                        + " was not found");
//            job.addDependency(resource, LockType.GENERATED);
//        }
//
//        // We have to wait for read lock resources to be generated
//        for (Object sharedLock : readLocks) {
//            Resource resource = scheduler.getResource(toResourceLocator(sharedLock));
//            if (resource == null)
//                throw new RuntimeException("Resource " + sharedLock
//                        + " was not found");
//            job.addDependency(resource, LockType.READ_ACCESS);
//        }
//
//        // Write locks
//        for (Object writeLock : writeLocks) {
//            final ResourceLocator id = toResourceLocator(writeLock);
//            Resource resource = scheduler.getResource(id);
//            if (resource == null) {
//                resource = new SimpleData(scheduler, id,
//                        LockMode.EXCLUSIVE_WRITER, false);
//            }
//            job.addDependency(resource, LockType.WRITE_ACCESS);
//        }
//
//        scheduler.store(job, null);
//        return true;
//    }

    static public class RPCArrayArgument implements RPCArgument {
        @Override
        public String name() {
            return null;
        }

        @Override
        public boolean required() {
            return true;
        }

        @Override
        public String help() {
            return "Array element";
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return RPCArgument.class;
        }
    }

    /**
     * A class that is used to control the environment in scripts
     *
     * @author B. Piwowarski <benjamin@bpiwowar.net>
     */
    static public class JSGetEnv {
        private final Map<String, String> environment;

        public JSGetEnv(Map<String, String> environment) {
            this.environment = environment;
        }

        public String get(String key) {
            return environment.get(key);
        }

        public String get(String key, String defaultValue) {
            String value = environment.get(key);
            if (value == null)
                return defaultValue;
            return value;
        }

    }

}
