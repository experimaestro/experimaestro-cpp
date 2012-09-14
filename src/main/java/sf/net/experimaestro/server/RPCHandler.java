/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

import com.sleepycat.je.DatabaseException;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Level;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.XmlRpcStreamServer;
import org.mortbay.jetty.Server;
import org.mozilla.javascript.*;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.locks.LockType;
import sf.net.experimaestro.manager.Repositories;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.manager.js.XPMObject;
import sf.net.experimaestro.scheduler.*;
import sf.net.experimaestro.utils.Output;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.Map.Entry;

/**
 * Our RPC handler for experimaestro
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class RPCHandler {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The task manager
     */
    private Scheduler scheduler;


    private XmlRpcRequest pRequest;
    /**
     * Server
     */
    private Server server;

    /**
     * The repository
     */
    private Repository repository;

    /**
     * Set the task server
     *
     * @param pRequest
     * @param scheduler
     */
    public void setTaskServer(XmlRpcRequest pRequest, Server server, Scheduler scheduler, Repository repository) {
        this.pRequest = pRequest;
        this.server = server;
        this.scheduler = scheduler;
        this.repository = repository;
    }


    /**
     * Update the status of jobs
     */
    public int updateJobs(Object[] states) throws Exception {
        final EnumSet<ResourceState> statesSet = getStates(states);

        int nbUpdated = 0;
        for (Resource resource : scheduler.resources(statesSet)) {
            if (resource.updateStatus())
                nbUpdated++;
        }

        return nbUpdated;
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
     * List jobs
     */
    public List<String> listJobs(Object[] states) {
        final EnumSet<ResourceState> set = getStates(states);
        List<String> list = new ArrayList<>();
        for(Resource resource: scheduler.resources(set)) {
            list.add(resource.toString() + "\t" + resource.getState().toString());
        }
        return list;
    }


    /**
     * Shutdown the server
     */
    public boolean shutdown() {
        // Close the scheduler
        scheduler.close();

        // Shutdown jetty (after 1s to allow this thread to finish)
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                server.setGracefulShutdown(1000);
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
                        }
                        System.exit(1);

                    }
            }
        }, 2000);

        // Everything's OK
        return true;
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
//		scheduler.add(new SimpleData(scheduler, identifier, LockMode.valueOf(mode),
//				exists));
//		return true;
//	}

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

    private volatile int index = 1;

    public void echo(String msg) {
        System.out.println(index + ": " + msg);
        index++;
    }


    /**
     * Run a javascript script (either the file or a string)
     * <p/>
     * This version is called from python scripts where maps would be marshalled
     * into a string. Instead, we get a list that we transform into a map.
     */
    public ArrayList<Object> runJSScript(boolean isFile, String content,
                                         Object[] envArray) {
        Map<String, String> environment = arrayToMap(envArray);
        return runJSScript(isFile, content, environment);
    }

    /**
     * Run a javascript script (either the file or a string)
     */
    public ArrayList<Object> runJSScript(boolean isFile, String content,
                                         Map<String, String> environment) {
        if (pRequest instanceof XmlRpcStreamServer) {
            final XmlRpcStreamServer request = (XmlRpcStreamServer) pRequest.getConfig();
            LOGGER.info("HERE I AM !!!!");
        }
        int error = 0;
        String errorMsg = "";
        XPMObject jsXPM = null;

        // Creates and enters a Context. The Context stores information
        // about the execution environment of a script.
        try {
            Context jsContext = Context
                    .enter();

            // Initialize the standard objects (Object, Function, etc.)
            // This must be done before scripts can be executed. Returns
            // a scope object that we use in later calls.
            Scriptable scope = jsContext.initStandardObjects();

            LOGGER.debug("Environment is: %s", Output.toString(", ",
                    environment.entrySet(),
                    new Output.Formatter<Entry<String, String>>() {
                        @Override
                        public String format(Entry<String, String> t) {
                            return String.format("%s: %s", t.getKey(),
                                    t.getValue());
                        }
                    }));


            ResourceLocator locator = new ResourceLocator(LocalhostConnector.getInstance(), isFile ? content : null);
            LOGGER.info("Script is %s", locator);

            // TODO: should be a one shot repository - ugly
            Repositories repositories = new Repositories(new ResourceLocator(LocalhostConnector.getInstance(), ""));
            repositories.add(repository, 0);

            ScriptableObject.defineProperty(scope, "env", new JSGetEnv(
                    environment), 0);
            jsXPM = new XPMObject(locator, jsContext, environment, scope, repositories,
                    scheduler);
            XPMObject.getLog().clear();

            final Object result;
            if (isFile)
                result = jsContext.evaluateReader(scope,
                        new FileReader(content), content, 1, null);
            else
                result = jsContext.evaluateString(scope, content, "stdin", 1,
                        null);

            if (result != null)
                LOGGER.debug("Returns %s", result.toString());
            else
                LOGGER.debug("Null result");

            // Object object = scope.get("Task", null);
            // if (object instanceof NativeFunction) {
            // org.mozilla.javascript.Context cx2 =
            // org.mozilla.javascript.Context
            // .enter();
            // ((NativeFunction) object).call(cx2, scope, scope, null);
            // org.mozilla.javascript.Context.exit();
            // }

        } catch (WrappedException e) {
            LOGGER.printException(Level.INFO, e.getCause());

            error = 2;
            errorMsg = e.getCause().toString();

            if (e.getCause() instanceof ExperimaestroRuntimeException) {
                ExperimaestroRuntimeException ee = (ExperimaestroRuntimeException) e
                        .getCause();
                List<String> context = ee.getContext();
                if (!context.isEmpty()) {
                    errorMsg += "\n[context]\n";
                    for (String s : ee.getContext()) {
                        errorMsg += s + "\n";
                    }
                }
            }
            errorMsg += "\n" + e.getScriptStackTrace();
        } catch (JavaScriptException e) {
            LOGGER.printException(Level.INFO, e);
            error = 3;
            errorMsg = e.toString();
            errorMsg += "\n" + e.getScriptStackTrace();
        } catch (Exception e) {
            LOGGER.printException(Level.INFO, e);
            error = 1;
            errorMsg = e.toString();
        } finally {
            // Exit context
            Context.exit();
        }

        ArrayList<Object> list = new ArrayList<Object>();
        list.add(error);
        list.add(errorMsg);
        if (jsXPM != null) {
            list.add(XPMObject.getLog());
        }
        XPMObject.resetLog();
        return list;
    }

    /**
     * Add a command line job
     *
     * @throws DatabaseException
     */
    public boolean runCommand(String name, int priority, Object[] command,
                              Object[] envArray, String workingDirectory, Object[] depends,
                              Object[] readLocks, Object[] writeLocks) throws DatabaseException {
        Map<String, String> env = arrayToMap(envArray);
        LOGGER.info(
                "Running command %s [%s] (priority %d); read=%s, write=%s; environment={%s}",
                name, Arrays.toString(command), priority,
                Arrays.toString(readLocks), Arrays.toString(writeLocks),
                Output.toString(", ", env.entrySet()));

        String[] commandArgs = new String[command.length];
        for (int i = command.length; --i >= 0; )
            commandArgs[i] = command[i].toString();

        Connector connector = LocalhostConnector.getInstance();
        CommandLineTask job = new CommandLineTask(scheduler, connector, name, commandArgs,
                env, new File(workingDirectory).getAbsolutePath());

        // XPMProcess locks
        for (Object depend : depends) {

            Resource resource = scheduler.getResource(toResourceLocator(depend));
            if (resource == null)
                throw new RuntimeException("Resource " + depend
                        + " was not found");
            job.addDependency(resource, LockType.GENERATED);
        }

        // We have to wait for read lock resources to be generated
        for (Object readLock : readLocks) {
            Resource resource = scheduler.getResource(toResourceLocator(readLock));
            if (resource == null)
                throw new RuntimeException("Resource " + readLock
                        + " was not found");
            job.addDependency(resource, LockType.READ_ACCESS);
        }

        // Write locks
        for (Object writeLock : writeLocks) {
            final ResourceLocator id = toResourceLocator(writeLock);
            Resource resource = scheduler.getResource(id);
            if (resource == null) {
                resource = new SimpleData(scheduler, id,
                        LockMode.EXCLUSIVE_WRITER, false);
                resource.register(job);
            }
            job.addDependency(resource, LockType.WRITE_ACCESS);
        }

        scheduler.add(job);
        return true;
    }

    private ResourceLocator toResourceLocator(Object depend) {
        throw new NotImplementedException();
//        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    /**
     * Utility function that transforms an array with paired values into a map
     *
     * @param envArray The array, must contain an even number of elements
     * @return a map
     */
    private Map<String, String> arrayToMap(Object[] envArray) {
        Map<String, String> env = new TreeMap<String, String>();
        for (Object x : envArray) {
            Object[] o = (Object[]) x;
            if (o.length != 2)
                // FIXME: should be a proper one
                throw new RuntimeException();
            env.put((String) o[0], (String) o[1]);
        }
        return env;
    }
}