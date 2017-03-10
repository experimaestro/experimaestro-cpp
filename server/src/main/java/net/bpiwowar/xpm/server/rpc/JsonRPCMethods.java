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
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.bpiwowar.xpm.connectors.NetworkShareAccess;
import net.bpiwowar.xpm.connectors.SingleHostConnector;
import net.bpiwowar.xpm.exceptions.CloseException;
import net.bpiwowar.xpm.exceptions.XPMCommandException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.fs.XPMFileSystemProvider;
import net.bpiwowar.xpm.fs.XPMPath;
import net.bpiwowar.xpm.scheduler.*;
import net.bpiwowar.xpm.utils.CloseableIterable;
import net.bpiwowar.xpm.utils.CloseableIterator;
import net.bpiwowar.xpm.utils.PathUtils;
import net.bpiwowar.xpm.utils.XPMInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
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
public class JsonRPCMethods extends BaseJsonRPCMethods {
    final static private Logger LOGGER = LogManager.getFormatterLogger();

    private static Multimap<String, RPCCaller> methods;

    private final JsonRPCSettings settings;

    /**
     * Listeners associated to this
     */
    HashSet<Listener> listeners = new HashSet<>();

    /**
     * Opened files
     */
    final Map<String, FileViewer> fileViewers = new HashMap<>();

    private HashMap<Class<?>, Object> objects = new HashMap<>();

    public JsonRPCMethods(JsonRPCSettings settings, boolean isWebSocket, JSONRPCRequest mos) throws IOException, NoSuchMethodException {
        super(mos);
        initMethods();
        this.settings = settings;
        addObjects(this, new DocumentationMethods(), new ExperimentsMethods(mos), new RPCObjects(this, settings));
    }

    public void addObjects(Object... objects) {
        for (Object object : objects) {
            this.objects.put(object.getClass(), object);
        }
    }

    public static void initMethods() throws IOException, NoSuchMethodException {
        if (methods == null) {
            methods = HashMultimap.create();

            // Add methods from other classes
            addRPCMethods(JsonRPCMethods.class);
            addRPCMethods(DocumentationMethods.class);
            addRPCMethods(ExperimentsMethods.class);
            RPCObjects.addRPCMethods(methods);
        }
    }

    public static void addRPCMethods(Class<?> jsonRPCMethodsClass) {
        final JsonRPCMethodsHolder def = jsonRPCMethodsClass.getAnnotation(JsonRPCMethodsHolder.class);

        // Add methods
        for (Method method : jsonRPCMethodsClass.getDeclaredMethods()) {
            final RPCMethod rpcMethod = method.getAnnotation(RPCMethod.class);
            if (rpcMethod != null) {
                String name = "".equals(rpcMethod.name()) ? method.getName() : rpcMethod.name();
                if (!def.value().isEmpty())
                    name = def.value() + "." + name;
                methods.put(name, new RPCMethodCaller(method));
            }
        }

        // Add classes
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
                LOGGER.warn("Error while handling JSON request", t);

                try {
                    mos.error(null, 1, "Could not parse JSON: " + t.getMessage());
                } catch (IOException e) {
                    LOGGER.error("Could not send the error message", e);
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

            for (RPCCaller candidate : candidates) {
                int score = candidate.score(p);

                if (score > max && score > Integer.MIN_VALUE) {
                    max = score;
                    argmax = candidate;
                }
            }

            if (argmax == null)
                throw new XPMCommandException("Cannot find a matching method for " + command);
            final Class<?> declaringClass = argmax.getDeclaringClass();

            Object result = argmax.call(objects.get(declaringClass), p);
            mos.endMessage(requestID, result);
        } catch (InvocationTargetException e) {
            try {
                Throwable t = e;
                while (t.getCause() != null) {
                    t = t.getCause();
                }
                LOGGER.info(() -> format("Error while handling JSON request [%s]", e.toString()), e);
                mos.error(requestID, 1, t.getMessage());
            } catch (IOException e2) {
                LOGGER.error("Could not send the return code", e2);
            }
        } catch (XPMRuntimeException t) {
            try {
                LOGGER.error("Error while running request", t);
                mos.error(requestID, 1, "Error while running request: " + t.toString());
            } catch (IOException e) {
                LOGGER.error("Could not send the return code");
            }
        } catch (Throwable t) {
            LOGGER.info("Internal error while handling JSON request", t);
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
        org.apache.logging.log4j.core.config.Configurator.setLevel(identifier, org.apache.logging.log4j.Level.toLevel(level));
        return 0;
    }

    /**
     * Shutdown the server
     */
    @RPCMethod(help = "Shutdown Experimaestro server")
    public boolean shutdown() {
        // Close the scheduler
        settings.scheduler.close();

        // Shutdown jetty (after 1s to allow this thread to finish)
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                boolean stopped = false;
                try {
                    settings.server.stop();
                    stopped = true;
                } catch (Exception e) {
                    LOGGER.error("Could not stop properly jetty", e);
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
    static public int invalidate(Resource resource, boolean restart) throws Exception {
        int nbUpdated = 0;
        try (final CloseableIterator<Dependency> deps = resource.getOutgoingDependencies(false)) {

            while (deps.hasNext()) {
                Dependency dependency = deps.next();
                final Resource to = dependency.getTo();
                LOGGER.info("Invalidating %s", to);

                invalidate(to, restart);

                final ResourceState state = to.getState();
                if (state == ResourceState.RUNNING)
                    ((Job) to).stop();
                if (!state.isActive()) {
                    nbUpdated++;
                    // We invalidate grand-children if the child was done
                    if (state == ResourceState.DONE) {
                        invalidate(to, restart);
                    }
                    ((Job) to).invalidate(restart);
                }
            }
        }
        return nbUpdated;
    }

    @RPCMethod(name = "cleanup-locks")
    static public void cleanupLocks(@RPCArgument(name = "simulate") boolean simulate) throws SQLException {
        Resource.cleanupLocks(simulate);
    }

    @RPCMethod(name = "invalidate", help = "Puts back a job into the waiting queue")
    public class Invalidate implements JsonCallable {
        @RPCArgument(name = "ids", help = "The id of the job(s) (string or integer)")
        String[] ids;

        @RPCArgument(name = "keep-done", help = "Whether done jobs should be invalidated")
        boolean keepDone;

        @RPCArgument(name = "recursive", required = false, help = "Whether we should invalidate dependent results when the job was done")
        boolean recursive = true;

        @RPCArgument(name = "restart", help = "Whether we should restart the job (false = error)")
        boolean restart = true;

        @Override
        public Integer call() throws Throwable {
            int nbUpdated = 0;

            for (String id : ids) {
                try {
                    Resource resource = getResource(id);
                    if (resource == null) {
                        LOGGER.error("Job not found [%s]", id);
                        continue;
                    }
                    LOGGER.info("Invalidating resource %s", resource);

                    final ResourceState rsrcState = resource.getState();

                    // Stop running jobs
                    if (rsrcState == ResourceState.RUNNING) {
                        ((Job) resource).stop();
                    }

                    // The job is active, so we have nothing to do
                    if (rsrcState.isActive() && rsrcState != ResourceState.RUNNING) {
                        // Just notify in case
                        Scheduler.notifyRunners();
                        continue;
                    }

                    if (keepDone && rsrcState == ResourceState.DONE)
                        continue;

                    ((Job) resource).invalidate(restart);
                    nbUpdated++;

                    // If the job was done, we need to invalidate the dependences
                    if (recursive && rsrcState == ResourceState.DONE) {
                        nbUpdated += invalidate(resource, restart);
                    }
                } catch (Throwable t) {
                    LOGGER.error("Could not invalidate job %s: %s", id, t);
                }
            }

            return nbUpdated;
        }
    }


    @RPCMethod
    public String hostname() {
        return settings.serverSettings.getName();
    }

    /**
     * Update the status of jobs
     */
    @RPCMethod(help = "Force the update of all the jobs statuses. Returns the number of jobs whose update resulted" +
            " in a change of state")
    public Map<String, Enum<ResourceState>> updateJobs(
            @RPCArgument(name = "resources", required = false) String[] resourceNames,
            @RPCArgument(name = "states", required = false) String[] statesNames
    ) throws Exception {
        EnumSet<ResourceState> states = getStates(statesNames);
        Map<String, Enum<ResourceState>> updated = new HashMap<>();

        if (resourceNames.length > 0) {
            for (String resourceName : resourceNames) {
                final Resource resource = getResource(resourceName);
                if (states.contains(resource.getState())) {
                    if (resource.updateStatus()) {
                        updated.put(resource.getIdentifier(), resource.getState());
                    }
                }
            }
            return updated;
        }

        try (final CloseableIterable<Resource> resources = settings.scheduler.resources(states)) {
            for (Resource resource : resources) {
                if (resource.updateStatus()) {
                    updated.put(resource.getIdentifier(), resource.getState());
                }
            }
        } catch (CloseException e) {
            throw new RuntimeException(e);
        }
        // Just in case
        Scheduler.notifyRunners();

        return updated;

    }

    @RPCMethod(help = "Returns the list of paths for a given task")
    public Map<String, String> paths(@RPCArgument(name = "id", required = true) String id) throws SQLException {
        final Resource resource = getResource(id);
        final Path locator = resource.getLocator();
        HashMap<String, String> map = new HashMap<>();

        if (locator instanceof XPMPath) {
            XPMPath _path = (XPMPath) locator;

            for (NetworkShareAccess access : XPMFileSystemProvider.getNetworkShareAccesses(_path)) {
                final SingleHostConnector connector = access.getConnector();
                final String hostPath = access.getPath();

                try {
                    Path path = connector.resolveFile(hostPath)
                            .resolve(_path.getLocalPath())
                            .normalize();
                    map.put(connector.getHostName(),
                            path.getParent()
                                    .toAbsolutePath()
                                    .toString());
                } catch (IOException e) {
                    LOGGER.warn("Cannot get path", e);
                }


            }
        } else {
            map.put("local", locator.getParent().toAbsolutePath().toString());
        }

        return map;
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
            try (final CloseableIterable<Resource> resources = settings.scheduler.resources(states)) {
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

    @RPCMethod(help = "Set token limit")
    public boolean setTokenLimit(
            @RPCArgument(name = "tokenId") String tokenId,
            @RPCArgument(name = "limit") int limit) throws SQLException {
        Resource resource = getResource(tokenId);
        if (resource == null) {
            throw new IllegalArgumentException(format("Not such resource id [%s]", tokenId));
        }

        if (!(resource instanceof TokenResource)) {
            throw new IllegalArgumentException(format("Resource [%s] is not a token", tokenId));
        }

        TokenResource tokenResource = (TokenResource) resource;
        tokenResource.setLimit(limit);
        return true;
    }


    @RPCMethod(help = "Listen to XPM events")
    public boolean listen() {
        Listener listener = message -> {
            try {
                // Just serialize and output
                mos.message(message);
            } catch (IOException e) {
                LOGGER.error("Could not output", e);
            } catch (RuntimeException e) {
                LOGGER.error("Error while trying to notify RPC client", e);
            }
        };

        listeners.add(listener);
        settings.scheduler.addListener(listener);
        return true;
    }

    public void close() {
        // Close all listeners
        for (Listener listener : listeners) {
            settings.scheduler.removeListener(listener);
        }

        // Close file viewers
        for (FileViewer fileViewer : fileViewers.values()) {
            try {
                fileViewer.close();
            } catch (IOException e) {
                LOGGER.error("Could not close %s", fileViewer);
            }
        }

        // Close other RPC handlers
        for (Object o : objects.values()) {
            if (o != this && o instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) o).close();
                } catch (Exception e) {
                    LOGGER.error(() -> format("while closing %s", o), e);
                }
            }
        }

        // Close streams
        super.close();
    }


    // File related methods
    @RPCMethod(name = "resource-path", help = "Get a resource related path")
    public String resourcePath(
            @RPCArgument(name = "id", help = "URI for file") String id,
            @RPCArgument(name = "type") String type
    ) throws IOException, SQLException {
        final Resource resource = getResource(id);
        switch (type) {
            case "stdout":
                return resource.outputFile().toAbsolutePath().toUri().toString();
            case "stderr":
                return resource.errorFile().toAbsolutePath().toUri().toString();
        }
        throw new IllegalArgumentException(format("No file of type %s", type));
    }

    @RPCMethod(name = "view-file", help = "View a part of a file")
    public String fileViewer(
            @RPCArgument(name = "uri", help = "URI for file") String uri,
            @RPCArgument(name = "position", help = "Position in file. If negative, relative to the end") long position,
            @RPCArgument(name = "size") int size) throws IOException {

        synchronized (fileViewers) {
            // Get the file viewer
            FileViewer fileViewer = fileViewers.get(uri);
            if (fileViewer == null) {
                fileViewers.put(uri, fileViewer = new FileViewer(uri));
            }

            final ByteBuffer bytes = fileViewer.read(position, size);

            return new String(bytes.array());
        }
    }

    @RPCMethod(name = "close-file")
    void closeFileViewer(@RPCArgument(name = "uri", help = "URI for file") String uri) throws IOException {
        synchronized (fileViewers) {
            final FileViewer fileViewer = fileViewers.get(uri);
            fileViewer.close();
            fileViewers.remove(uri);
        }
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
        try (final CloseableIterable<Resource> resources = settings.scheduler.resources(statesSet)) {
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
        for (String id : JobIds) {
            try {
                final Resource resource = getResource(id);
                if (resource instanceof Job) {
                    ((Job) resource).generateFiles();
                }
            } catch (Throwable throwable) {
                LOGGER.error(() -> format("Could not generate files for resource [%s]", id), throwable);
            }
        }
    }

    @RPCMethod(name = "kill", help = "Kill one or more jobs")
    public class Kill implements JsonCallable {
        @RPCArgument
        String jobs[];

        @Override
        public Object call() throws Throwable {
            int n = 0;
            for (String id : jobs) {
                try {
                    final Resource resource = getResource(id);
                    if (resource instanceof Job) {
                        if (((Job) resource).stop()) {
                            n++;
                        }
                    }
                } catch (Throwable throwable) {
                    LOGGER.error("Error while killing jbo [%s]", id);
                }
            }
            return n;
        }
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

        try (final CloseableIterable<Resource> resources = settings.scheduler.resources(set)) {
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

    @RPCMethod(help = "Get ssh-agent information")
    public JsonObject sshAgentInformation() {
        JsonObject object = new JsonObject();
        object.addProperty("SSH_AUTH_SOCK", System.getenv("SSH_AUTH_SOCK"));
        object.addProperty("SSH_AGENT_PID", System.getenv("SSH_AGENT_PID"));
        return object;
    }

    @RPCMethod(help = "Change path prefix of resources")
    public int changeResourcesPrefix(
            @RPCArgument(name = "old") String oldPrefix,
            @RPCArgument(name = "new") String newPrefix) throws SQLException, CloseException, IOException {
        int count = 0;
        int oldLength = oldPrefix.length();
        try (CloseableIterable<Resource> resources = Scheduler.get().resources().find(
                Resource.SELECT_BEGIN + " WHERE LEFT(path,?) = ?",
                st -> {
                    st.setInt(1, oldLength);
                    st.setString(2, oldPrefix);
                })) {
            for (Resource resource : resources) {
                String locator = PathUtils.normalizedString(resource.getLocator());
                if (!locator.substring(0, oldLength).equals(oldPrefix)) {
                    LOGGER.warn("Prefixes [%s] and [%s] do not match", locator.substring(0, oldLength), oldPrefix);
                } else {
                    resource.setLocator(PathUtils.toPath(newPrefix + locator.substring(oldLength)));
                }

            }

        }
        return count;
    }


}
