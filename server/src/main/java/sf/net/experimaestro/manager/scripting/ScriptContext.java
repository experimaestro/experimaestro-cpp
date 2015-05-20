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
import sf.net.experimaestro.manager.experiments.TaskReference;
import sf.net.experimaestro.manager.plans.Operator;
import sf.net.experimaestro.manager.plans.TaskOperator;
import sf.net.experimaestro.manager.plans.Value;
import sf.net.experimaestro.scheduler.Dependency;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.CachedIterable;
import sf.net.experimaestro.utils.Cleaner;
import sf.net.experimaestro.utils.Updatable;
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
    Updatable<Path> workingDirectory;

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
    private Updatable<Long> experimentId;

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
     * Properties set by the script that will be returned
     */
    Map<String, Object> properties;

    /**
     * Environment
     */
    Updatable<Map<String, String>> environment;

    /**
     * Submitted jobs
     *
     * @param staticContext
     */
    Map<String, Resource> submittedJobs;

    public ScriptContext(StaticContext staticContext) {

        if (threadContext.get() != null)
            throw new IllegalStateException("Cannot create a new script context if another one is active");

        LOGGER.debug("Creating script context [%s] from static context", this);

        this.staticContext = staticContext;
        this.cleaner = new Cleaner();

        defaultLocks = new Updatable<>(new HashMap<>(), x -> new HashMap(x));
        experimentId = Updatable.create(null);
        priority = Updatable.create(0);
        simulate = Updatable.create(false);
        workingDirectory = Updatable.create(null);
        defaultLauncher = Updatable.create(new DirectLauncher());
        threadContext.set(this);
        connector = Updatable.create(null);
        currentScriptPath = Updatable.create(null);
        environment = Updatable.create(new HashMap<>());
        properties = new HashMap<>();
        submittedJobs = new HashMap<>();
    }

    @Override
    public String toString() {
        return format("ScriptContext@%X", System.identityHashCode(this));
    }

    private ScriptContext(ScriptContext other, boolean newRepository) {
        LOGGER.debug("Creating script context [%s] from context [%s]", this, other);

        staticContext = other.staticContext;
        cleaner = other.cleaner;
        counts = other.counts;

        defaultLocks = other.defaultLocks.reference();
        experimentId = other.experimentId.reference();
        priority = other.priority.reference();
        simulate = other.simulate.reference();
        workingDirectory = other.workingDirectory.reference();
        defaultLauncher = other.defaultLauncher.reference();
        connector = other.connector.reference();
        currentScriptPath = other.currentScriptPath.reference();
        environment = other.environment.reference();

        counts = other.counts;
        submittedJobs = other.submittedJobs;

        if (newRepository) {
            properties = new HashMap<>();
        } else {
            properties = other.properties;
        }

        // Sets the current thread context
        oldCurrent = threadContext.get();
        threadContext.set(this);
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

    public void startedJob(Job job) {
        // Notify listeners that a job has started
        for (Consumer<Job> listener : newTaskListeners) {
            listener.accept(job);
        }

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
        return new ScriptContext(this, false);
    }

    public ScriptContext copy(boolean newRepository) {
        return new ScriptContext(this, newRepository);
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

        return workingDirectory.get();
    }

    public void setWorkingDirectory(Path workingDirectory) {

        this.workingDirectory.set(workingDirectory);
    }

    public Long getExperimentId() {

        return experimentId.get();
    }

    public void setExperimentId(long experimentId) {

        this.experimentId.set(experimentId);
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
        if (factory == null)
            throw new XPMRuntimeException("Could not find a task with name [%s]", qname);
        LOGGER.info("Creating a new JS task [%s]", factory.getId());
        return factory.create();
    }

    public void setCurrentScriptPath(Path currentScriptPath) {
        this.currentScriptPath.set(currentScriptPath);
    }

    public String setEnv(String key, String value) {
        return environment.modify().put(key, value);
    }

    public String getEnv(String key) {
        return environment.get().get(key);
    }
}
