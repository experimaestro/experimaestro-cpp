package sf.net.experimaestro.manager.scripting;

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

import org.apache.commons.lang.mutable.MutableInt;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.DirectLauncher;
import sf.net.experimaestro.connectors.Launcher;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.manager.experiments.Experiment;
import sf.net.experimaestro.manager.experiments.TaskReference;
import sf.net.experimaestro.manager.plans.Operator;
import sf.net.experimaestro.manager.plans.TaskOperator;
import sf.net.experimaestro.manager.plans.Value;
import sf.net.experimaestro.scheduler.Dependency;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.*;
import sf.net.experimaestro.utils.log.Logger;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.String.format;

/**
 * Context when running a script
 * <p>
 * This class hides away what is part of the static context and
 * what if part of the dynamic one
 */
final public class ScriptContext implements AutoCloseable {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The thread local context
     */
    private final static ThreadLocal<ScriptContext> threadContext = new ThreadLocal<>();

    /**
     * The script static context
     */
    final StaticContext staticContext;

    /**
     * Default locks
     */
    Updatable<Map<Resource, Object>> defaultLocks;

    /**
     * Priority
     */
    Updatable<Integer> priority;

    /**
     * The working directory
     */
    Mutable<Path> workingDirectory;

    /**
     * List of listeners for new jobs
     */
    ArrayList<Consumer<Job>> newTaskListeners = new ArrayList<>();

    /**
     * The resource cleaner
     * <p>
     * Used to close objects at the end of the execution of a script
     */
    Cleaner cleaner;

    /**
     * Previous context
     */
    private ScriptContext oldCurrent = null;

    /**
     * Counts the number of items output by an operator; null if not used
     */
    private Map<Operator, MutableInt> counts;

    /**
     * Whether we should simulate
     */
    private Updatable<Boolean> simulate;

    /**
     * The default launcher
     */
    private Updatable<Launcher> defaultLauncher;

    /**
     * The default connector
     */
    private Updatable<Connector> connector;

    /**
     * Associated experiment
     */
    private Updatable<Experiment> experiment;

    /**
     * The current task
     */
    private TaskReference task;

    /**
     * The current script path
     */
    private Updatable<Path> currentScriptPath;

    /**
     * Cached iterators
     */
    private IdentityHashMap<Object, CachedIterable<Value>> cachedIterables = new IdentityHashMap<>();

    /**
     * The task operator maps between
     */
    private IdentityHashMap<TaskOperator, TaskReference> taskOperatorMap = new IdentityHashMap<>();

    /**
     * Parameters
     */
    MapStack<String, String> parameters;

    /**
     * Properties set by the script that will be returned
     */
    Map<String, Object> properties;

    /**
     * Submitted jobs
     *
     * @param staticContext
     */
    Map<String, Resource> submittedJobs;

    public ScriptContext(StaticContext staticContext) {
        if (threadContext.get() != null) {
            throw new IllegalStateException("Cannot create a new script context if another one is active");
        }

        LOGGER.debug("Creating script context [%s] from static context", this);

        this.staticContext = staticContext;
        this.cleaner = new Cleaner();

        defaultLocks = new Updatable<>(new HashMap<>(), x -> new HashMap(x));
        experiment = Updatable.create(null);
        priority = Updatable.create(0);
        simulate = Updatable.create(false);
        workingDirectory = new Mutable<>();
        defaultLauncher = Updatable.create(new DirectLauncher(Scheduler.get().getLocalhostConnector()));
        threadContext.set(this);
        connector = Updatable.create(Scheduler.get().getLocalhostConnector());
        currentScriptPath = Updatable.create(null);
        properties = new HashMap<>();
        submittedJobs = new HashMap<>();
        parameters = new MapStack<>();
    }

    @Override
    public String toString() {
        return format("ScriptContext@%X", System.identityHashCode(this));
    }

    private ScriptContext(ScriptContext other, boolean newRepository, boolean setThreadContext) {
        LOGGER.debug("Creating script context [%s] from context [%s]", this, other);

        staticContext = other.staticContext;
        cleaner = other.cleaner;
        counts = other.counts;

        experiment = other.experiment.reference();
        priority = other.priority.reference();
        simulate = other.simulate.reference();
        connector = other.connector.reference();
        currentScriptPath = other.currentScriptPath.reference();

        // Copy global values
        counts = other.counts;
        submittedJobs = other.submittedJobs;

        // Initialise shared values
        if (newRepository) {
            properties = new HashMap<>();
            workingDirectory = new Mutable<>();
            defaultLauncher = other.defaultLauncher.reference();
            defaultLocks = other.defaultLocks.reference();
            parameters = new MapStack<>(other.parameters);
        } else {
            properties = other.properties;
            workingDirectory = other.workingDirectory;
            defaultLauncher = other.defaultLauncher;
            defaultLocks = other.defaultLocks;
        }

        if (setThreadContext) {
            // Sets the current thread context
            oldCurrent = threadContext.get();
            threadContext.set(this);
        }
    }


    static public ScriptContext get() {

        return threadContext.get();
    }

    public ScriptContext counts(boolean flag) {

        if (flag) counts = new HashMap<>();
        else counts = null;
        return this;
    }

    public Map<Operator, MutableInt> counts() {

        return counts;
    }

    public boolean simulate() {

        return simulate.get();
    }

    public void setCachedIterable(Object key, CachedIterable<Value> cachedIterable) {

        cachedIterables.put(key, cachedIterable);
    }

    public CachedIterable<Value> getCachedIterable(Object key) {

        return cachedIterables.get(key);
    }

    public void setTaskOperatorMap(IdentityHashMap<TaskOperator, TaskReference> taskOperatorMap) {

        this.taskOperatorMap = taskOperatorMap;
    }

    /**
     * Set the current task operator
     */
    public void setTaskOperator(TaskOperator taskOperator) {

        setTask(taskOperator == null ? null : taskOperatorMap.get(taskOperator));
    }

    public ScriptContext defaultLocks(Map<Resource, Object> defaultLocks) {
        this.defaultLocks.set(defaultLocks);
        return this;
    }

    public Map<Resource, Object> defaultLocks() {

        return defaultLocks.get();
    }

    public ScriptContext simulate(boolean simulate) {

        this.simulate.set(simulate);
        return this;
    }

    public Scheduler getScheduler() {

        return staticContext.scheduler;
    }

    public void setTask(TaskReference task) {

        this.task = task;
    }

    public TaskReference getTaskReference() {

        return task;
    }

    public Logger getMainLogger() {
        return staticContext.getMainLogger();
    }

    public Logger getLogger(String loggerName) {
        return (Logger) staticContext.loggerRepository.getLogger(loggerName, Logger.factory());
    }

    /**
     * Prepares the task with the current task context
     *
     * @param resource
     */
    public void prepare(Resource resource) {
        // -- Adds default locks
        for (Map.Entry<? extends Resource, ?> lock : defaultLocks.get().entrySet()) {
            Dependency dependency = lock.getKey().createDependency(lock.getValue());
            ((Job) resource).addDependency(dependency);
        }

        if (defaultLauncher.get() != null) {
            resource.setLauncher(defaultLauncher.get());
        }
    }

    public ScriptContext addNewTaskListener(Consumer<Job> listener) {

        newTaskListeners.add(listener);
        return this;
    }

    public ScriptContext addDefaultLocks(Map<Resource, Object> map) {

        defaultLocks.modify().putAll(map);
        return this;
    }

    public ScriptContext copy() {
        return new ScriptContext(this, false, true);
    }

    public ScriptContext copy(boolean newRepository) {
        return new ScriptContext(this, newRepository, true);
    }

    public ScriptContext copy(boolean newRepository, boolean setThreadContext) {
        return new ScriptContext(this, newRepository, setThreadContext);
    }

    public TaskFactory getFactory(QName qName) {

        return staticContext.repository.getFactory(qName);
    }

    public Repository getRepository() {

        return staticContext.repository;
    }

    public Map<Resource, Object> getDefaultLocks() {

        return defaultLocks.get();
    }

    public int getPriority() {

        return priority.get();
    }

    public void setPriority(int priority) {

        this.priority.set(priority);
    }

    public Path getWorkingDirectory() {
        return workingDirectory.getValue();
    }

    public void setWorkingDirectory(Path workingDirectory) {
        LOGGER.debug("[%s] Setting working directory to %s", this, workingDirectory);
        this.workingDirectory.setValue(workingDirectory);
    }

    public Experiment getExperiment() {
        return experiment.get();
    }

    public void setExperiment(Experiment experiment) {
        this.experiment.set(experiment);
    }

    public Cleaner getCleaner() {

        return cleaner;
    }

    @Override
    public void close() {

        if (threadContext.get() != this) {
            LOGGER.error("Current thread context [%s] is not ourselves [%s]", threadContext.get(), this);
        }

        LOGGER.debug("Closing script context [%s] - restoring [%s]", this, oldCurrent);

        threadContext.set(oldCurrent);
        if (oldCurrent == null) {
            cleaner.close();
        }
    }

    public void addDefaultLock(Resource resource, Object parameters) {

        defaultLocks.modify().put(resource, parameters);
    }

    public Launcher getDefaultLauncher() {

        return defaultLauncher.get();
    }

    public void setDefaultLauncher(Launcher defaultLauncher) {

        this.defaultLauncher.set(defaultLauncher);
    }

    public Connector getConnector() {
        return connector.get();
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public Path getCurrentScriptPath() {
        return currentScriptPath.get();
    }


    public void register(Closeable closeable) {
        cleaner.register(closeable);
    }

    public void unregister(AutoCloseable autoCloseable) {
        cleaner.unregister(autoCloseable);
    }


    public Task getTask(QName qname) {
        TaskFactory factory = getFactory(qname);
        if (factory == null) {
            throw new XPMRuntimeException("Could not find a task with name [%s]", qname);
        }
        LOGGER.info("Creating a new JS task [%s]", factory.getId());
        return factory.create();
    }

    public void setCurrentScriptPath(Path currentScriptPath) {
        this.currentScriptPath.set(currentScriptPath);
    }

    // Only used for tests
    public static void force(ScriptContext sc) {
        threadContext.set(sc);
    }

    public void setParameter(String key, String value) {
        parameters.put(key, value);
    }

    /**
     * Swap the thread-local script context with this one
     *
     * Useful for a try-resource block where we want the old context to be set back.
     * @return A closeable object that will put back the context
     */
    public Swap swap() {
        return new Swap();
    }

    public class Swap implements Closeable {
        private final ScriptContext old;

        Swap() {
            old = threadContext.get();
            threadContext.set(ScriptContext.this);
        }

        @Override
        public void close()  {
            if (threadContext.get() != ScriptContext.this) {
                LOGGER.error("Current thread context [%s] is not ourselves [%s]", threadContext.get(), ScriptContext.this);
            }
            threadContext.set(old);
        }
    }
}
