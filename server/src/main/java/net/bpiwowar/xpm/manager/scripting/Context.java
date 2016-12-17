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
import net.bpiwowar.xpm.exceptions.XPMScriptRuntimeException;
import net.bpiwowar.xpm.manager.TypeName;
import net.bpiwowar.xpm.manager.experiments.Experiment;
import net.bpiwowar.xpm.manager.experiments.SubmittedJob;
import net.bpiwowar.xpm.manager.experiments.TaskReference;
import net.bpiwowar.xpm.scheduler.*;
import net.bpiwowar.xpm.utils.*;
import net.bpiwowar.xpm.utils.log.Logger;
import org.apache.log4j.spi.LoggerRepository;

import java.io.Closeable;
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
final public class Context implements AutoCloseable {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The thread local context
     */
    private final static ThreadLocal<Context> threadContext = new ThreadLocal<>();

    /**
     * Default locks
     */
    Map<Resource, DependencyParameters> defaultLocks;

    /**
     * Priority
     */
    Integer priority;

    /**
     * The working directory
     */
    Path workingDirectory;

    /**
     * List of listeners for new jobs
     */
    ArrayList<Consumer<Job>> newTaskListeners = new ArrayList<>();

    /**
     * The default launcher
     */
    private Launcher defaultLauncher;

    /**
     * Associated experiment
     */
    private Experiment experiment;

    /**
     * Parameters
     */
    MapStack<Object, Object> parameters;

    /**
     * Properties set by the script that will be returned
     */
    Map<String, Object> properties;

    /**
     * Submitted jobs
     */
    private Map<String, SubmittedJob> submittedJobs;
    /**
     * The scheduler
     */
    Scheduler scheduler;

    /**
     * The logger
     */
    LoggerRepository loggerRepository;

    /**
     * Main logger
     */
    final Logger mainLogger;

    /**
     * The resource cleaner
     * <p>
     * Used to close objects at the end of the execution of a script
     */
    Cleaner cleaner;


    public void register(Closeable closeable) {
        cleaner.register(closeable);
    }

    public void unregister(AutoCloseable autoCloseable) {
        cleaner.unregister(autoCloseable);
    }

    public Logger getMainLogger() {
        return mainLogger;
    }

    @Override
    public void close() {
        threadContext.remove();
        cleaner.close();
    }
    /**
     * Whether we are simulating
     */
    boolean simulate;


    public Context(Scheduler scheduler, LoggerRepository loggerRepository) {
        LOGGER.debug("Creating script context [%s] from static context", this);

        this.scheduler = scheduler;
        this.loggerRepository = loggerRepository;
        this.mainLogger = (Logger)loggerRepository.getLogger("xpm", Logger.factory());
        this.cleaner = new Cleaner();

        defaultLocks = new HashMap<>();
        experiment = null;
        priority = 0;
        workingDirectory = null;
        defaultLauncher = new DirectLauncher(Scheduler.get().getLocalhostConnector());
        threadContext.set(this);
        properties = new HashMap<>();
        parameters = new MapStack<>();

        simulate = false;
        submittedJobs = new HashMap<>();
    }

    @Override
    public String toString() {
        return format("Context@%X", System.identityHashCode(this));
    }


    static public Context get() {
        return threadContext.get();
    }

    public Context defaultLocks(Map<Resource, DependencyParameters> defaultLocks) {
        this.defaultLocks = defaultLocks;
        return this;
    }

    public Map<Resource, DependencyParameters> defaultLocks() {
        return defaultLocks;
    }


    public Scheduler getScheduler() {
        return scheduler;
    }

    public Logger getLogger(String loggerName) {
        return (Logger) loggerRepository.getLogger(loggerName, Logger.factory());
    }

    /**
     * Prepares the task with the current task context
     *
     * @param resource The resource to prepare
     */
    public void prepare(Resource resource) {
        // -- Adds default locks
        for (Map.Entry<? extends Resource, DependencyParameters> lock : defaultLocks.entrySet()) {
            Dependency dependency = lock.getKey().createDependency(lock.getValue());
            ((Job) resource).addDependency(dependency);
        }

        if (defaultLauncher != null) {
            Launcher launcher = defaultLauncher;
            resource.setLauncher(launcher, (LauncherParameters) parameters.get(launcher));
        }
    }

    public Context addNewTaskListener(Consumer<Job> listener) {
        newTaskListeners.add(listener);
        return this;
    }

    public Context addDefaultLocks(Map<Resource, DependencyParameters> map) {
        defaultLocks.putAll(map);
        return this;
    }

    public Map<Resource, DependencyParameters> getDefaultLocks() {
        return defaultLocks;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Path getWorkingDirectory() {
        return workingDirectory; }

    public void setWorkingDirectory(Path workingDirectory) {
        LOGGER.debug("[%s] Setting working directory to %s", this, workingDirectory);
        this.workingDirectory = workingDirectory;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }

    public void addDefaultLock(Resource resource, DependencyParameters parameters) {
        defaultLocks.put(resource, parameters);
    }

    public Launcher getDefaultLauncher() {
        return defaultLauncher;
    }

    public void setDefaultLauncher(Launcher defaultLauncher) {
        this.defaultLauncher = defaultLauncher;
    }

    public Connector getConnector() {
        if (defaultLauncher == null) {
            return scheduler.getLocalhostConnector();
        }
        return defaultLauncher.getConnector();
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }


    public void setParameter(Object key, Object value) {
        parameters.put(key, value);
    }

    public Object getParameter(Object key) {
        return parameters.get(key);
    }


    /**
     * Post processing of a saved resource
     *
     * @param resource The saved resource
     */
    public void postProcess(Resource resource) {
        // --- Build the experiment task hierarchy and set the resource position within it
        TaskReference taskReference = null;
        if (getExperiment() == null) {
            throw new XPMScriptRuntimeException("Experiment is not set");
        }

        try {
            // Find the task reference with the same ID that has the same parents,
            // otherwise create a new task reference
            String _taskId = resource.taskId();
            if (_taskId == null) {
                _taskId = "?";
            }
            final String taskId = _taskId;

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
                taskReference = new TaskReference(taskId, experiment, parentTaskReferences);
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

    /**
     * Submitted jobs
     */
    public Map<String, SubmittedJob> getSubmittedJobs() {
        return submittedJobs;
    }

    public boolean simulate() {
        return simulate;
    }

    public Context simulate(boolean simulate) {
        this.simulate = simulate;
        return this;
    }

    public static Logger mainLogger() {
        final Context sc = Context.get();
        if (sc == null) return LOGGER;
        return sc.getMainLogger();
    }


    public void setThreadScriptContext() {
        threadContext.set(this);
    }
}
