package net.bpiwowar.xpm.manager.scripting;

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

import net.bpiwowar.xpm.connectors.Connector;
import net.bpiwowar.xpm.connectors.DirectLauncher;
import net.bpiwowar.xpm.connectors.Launcher;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.exceptions.XPMScriptRuntimeException;
import net.bpiwowar.xpm.manager.DummyTask;
import net.bpiwowar.xpm.manager.TypeName;
import net.bpiwowar.xpm.manager.Repository;
import net.bpiwowar.xpm.manager.Task;
import net.bpiwowar.xpm.manager.TaskFactory;
import net.bpiwowar.xpm.manager.experiments.Experiment;
import net.bpiwowar.xpm.manager.experiments.SubmittedJob;
import net.bpiwowar.xpm.manager.experiments.TaskReference;
import net.bpiwowar.xpm.manager.json.JsonSimple;
import net.bpiwowar.xpm.scheduler.*;
import net.bpiwowar.xpm.utils.IdentityHashSet;
import net.bpiwowar.xpm.utils.MapStack;
import net.bpiwowar.xpm.utils.Mutable;
import net.bpiwowar.xpm.utils.Updatable;
import net.bpiwowar.xpm.utils.log.Logger;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
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
    Updatable<Map<Resource, DependencyParameters>> defaultLocks;

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
     * Previous context
     */
    private ScriptContext oldCurrent = null;

    /**
     * The default launcher
     */
    private Updatable<Launcher> defaultLauncher;

    /**
     * Associated experiment
     */
    private Updatable<Experiment> experiment;

    /**
     * The current script path
     */
    private Updatable<Path> currentScriptPath;

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
     */
    private Map<String, SubmittedJob> submittedJobs;

    /**
     * Whether we are simulating
     */
    Updatable<Boolean> simulate;

    /**
     * Current task
     */
    private Updatable<Task> task;


    public ScriptContext(StaticContext staticContext) {
        if (threadContext.get() != null) {
            throw new IllegalStateException("Cannot create a new script context if another one is active");
        }

        LOGGER.debug("Creating script context [%s] from static context", this);

        this.staticContext = staticContext;

        defaultLocks = new Updatable<>(new HashMap<>(), HashMap::new);
        task = Updatable.create(null);
        experiment = Updatable.create(null);
        priority = Updatable.create(0);
        workingDirectory = new Mutable<>();
        defaultLauncher = Updatable.create(new DirectLauncher(Scheduler.get().getLocalhostConnector()));
        threadContext.set(this);
        currentScriptPath = Updatable.create(null);
        properties = new HashMap<>();
        parameters = new MapStack<>();

        simulate = new Updatable<>(false);
        submittedJobs = new HashMap<>();


    }

    @Override
    public String toString() {
        return format("ScriptContext@%X", System.identityHashCode(this));
    }

    private ScriptContext(ScriptContext parent, boolean newRepository, boolean setThreadContext) {
        LOGGER.debug("Creating script context [%s] from context [%s]", this, parent);

        staticContext = parent.staticContext;
        experiment = parent.experiment.reference();
        priority = parent.priority.reference();
        currentScriptPath = parent.currentScriptPath.reference();

        // Initialise shared values
        if (newRepository) {
            properties = new HashMap<>();
            workingDirectory = new Mutable<>(parent.workingDirectory.getValue());
            defaultLauncher = parent.defaultLauncher.reference();
            defaultLocks = parent.defaultLocks.reference();
            parameters = new MapStack<>(parent.parameters);
        } else {
            properties = parent.properties;
            workingDirectory = parent.workingDirectory;
            defaultLauncher = parent.defaultLauncher;
            defaultLocks = parent.defaultLocks;
        }


        simulate = parent.simulate.reference();
        submittedJobs = parent.submittedJobs;
        task = parent.task.reference();

        if (setThreadContext) {
            // Sets the current thread context
            oldCurrent = threadContext.get();
            threadContext.set(this);
        }
    }


    static public ScriptContext get() {
        return threadContext.get();
    }


    public ScriptContext defaultLocks(Map<Resource, DependencyParameters> defaultLocks) {
        this.defaultLocks.set(defaultLocks);
        return this;
    }

    public Map<Resource, DependencyParameters> defaultLocks() {
        return defaultLocks.get();
    }


    public Scheduler getScheduler() {

        return staticContext.scheduler;
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
     * @param resource The resource to prepare
     */
    public void prepare(Resource resource) {
        // -- Adds default locks
        for (Map.Entry<? extends Resource, DependencyParameters> lock : defaultLocks.get().entrySet()) {
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

    public ScriptContext addDefaultLocks(Map<Resource, DependencyParameters> map) {
        defaultLocks.modify().putAll(map);
        return this;
    }

    public ScriptContext copy() {
        return new ScriptContext(this, false, true);
    }

    public ScriptContext copy(boolean newRepository, boolean setThreadContext) {
        return new ScriptContext(this, newRepository, setThreadContext);
    }

    public TaskFactory getFactory(TypeName typeName) {

        return staticContext.repository.getFactory(typeName);
    }

    public Repository getRepository() {

        return staticContext.repository;
    }

    public Map<Resource, DependencyParameters> getDefaultLocks() {
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

    @Override
    public void close() {

        if (threadContext.get() != this) {
            LOGGER.error("Current thread context [%s] is not ourselves [%s]", threadContext.get(), this);
        }

        LOGGER.debug("Closing script context [%s] - restoring [%s]", this, oldCurrent);

        threadContext.set(oldCurrent);
    }

    public void addDefaultLock(Resource resource, DependencyParameters parameters) {
        defaultLocks.modify().put(resource, parameters);
    }

    public Launcher getDefaultLauncher() {
        return defaultLauncher.get();
    }

    public void setDefaultLauncher(Launcher defaultLauncher) {
        this.defaultLauncher.set(defaultLauncher);
    }

    public Connector getConnector() {
        Launcher launcher = defaultLauncher.get();
        if (launcher == null) {
            return Scheduler.get().getLocalhostConnector();
        }
        return launcher.getConnector();
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public Path getCurrentScriptPath() {
        return currentScriptPath.get();
    }

    public Task getTask(TypeName qname) {
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

    public String getParameter(String key) {
        return parameters.get(key);
    }


    /**
     * Post processing of a saved resource
     *
     * @param task The corresponding task
     * @param resource The saved resource
     */
    public void postProcess(Task task, Resource resource) {
        // --- Add tags

        if (task != null) {
            final Map<String, JsonSimple> tags = task.tags();
            if (!tags.isEmpty()) {
                try (XPMStatement st = Scheduler.statement("INSERT INTO ResourceTags(resource,tag,value) VALUES(?,?,?)")) {
                    st.setLong(1, resource.getId());
                    for (Map.Entry<String, JsonSimple> entry : tags.entrySet()) {
                        st.setString(2, entry.getKey());
                        st.setString(3, entry.getValue().get().toString());
                        st.execute();
                    }

                } catch (SQLException e) {
                    LOGGER.error(e, "Could not save resource tags");
                }
            }
        }

        // --- Build the experiment task hierarchy and set the resource position within it

        // Get the current task if needed
        if (task == null) {
            task = this.task.get();
        }

        TaskReference taskReference = null;
        if (getExperiment() == null) {
            throw new XPMScriptRuntimeException("Experiment is not set");
        }

        try {
            // If no task, use a dummy one
            if (task == null) {
                // Create dummy task
                task = DummyTask.INSTANCE;
            }

            // Find the task reference with the same ID that has the same parents,
            // otherwise create a new task reference
            final TypeName taskId = task.getFactory().getId();
            IdentityHashSet<TaskReference> set = null;
            ArrayList<TaskReference> parentTaskReferences = new ArrayList<>();
            for (Dependency dependency : resource.getDependencies()) {
                final SubmittedJob dep = submittedJobs.get(dependency.getFrom().getLocator().toString());
                if (dep == null) {
                    continue; // Might be a task without any experiment
                }
                parentTaskReferences.add(dep.taskReference);

                if (set == null) {
                    set = new IdentityHashSet<>();
                    dep.taskReference.children().stream()
                            .filter(tr -> tr.getTaskId().equals(taskId))
                            .forEach(set::add);
                } else {
                    IdentityHashSet<TaskReference> newSet = new IdentityHashSet<>();
                    dep.taskReference.children().stream()
                            .filter(set::contains)
                            .filter(tr -> tr.getTaskId().equals(taskId))
                            .forEach(newSet::add);
                    set = newSet;
                }

                if (set.isEmpty()) break;
            }

            // Create a new task reference if necessary
            if (set == null || set.isEmpty()) {
                taskReference = new TaskReference(taskId, experiment.get(), parentTaskReferences);
                taskReference.save();
            } else {
                taskReference = set.iterator().next();
            }

            // Add resource
            taskReference.add(resource);
        } catch (SQLException e) {
            // FIXME: do something better?
            getMainLogger().error(e, "Error while registering experiment");
        }

        final SubmittedJob submittedJob = new SubmittedJob(resource, taskReference);
        getSubmittedJobs().put(resource.getLocator().toString(), submittedJob);

    }

    public void setTask(Task task) {
        this.task.set(task);
    }

    /**
     * Submitted jobs
     */
    public Map<String, SubmittedJob> getSubmittedJobs() {
        return submittedJobs;
    }

    public boolean simulate() {
        return simulate.get();
    }

    public ScriptContext simulate(boolean simulate) {
        this.simulate.set(simulate);
        return this;
    }

    public static Logger mainLogger() {
        final ScriptContext sc = ScriptContext.get();
        if (sc  == null) return LOGGER;
        return sc.getMainLogger();
    }
}
