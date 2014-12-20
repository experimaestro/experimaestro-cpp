package sf.net.experimaestro.manager;

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

import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import sf.net.experimaestro.manager.experiments.Experiment;
import sf.net.experimaestro.manager.experiments.TaskReference;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The context when running a task
 *
 * @author B. Piwowarski
 */
public class TaskContext {
    /**
     * The logger
     */
    private final Logger logger;
    /**
     * The default locks
     */
    public Map<Resource, String> defaultLocks = ImmutableMap.of();
    /**
     * The working directory
     */
    public Path workingDirectory;
    /**
     * The scheduler
     */
    private Scheduler scheduler;
    /**
     * Whether we should simulate
     */
    private boolean simulate;
    /**
     * Resource path
     */
    private Path path;
    /**
     * List of listeners for new jobs
     */
    ArrayList<Consumer<Job>> newTaskListeners = new ArrayList<>();
    /**
     * Associated experiment
     */
    private Long experimentId;

    /**
     * The current task
     */
    private TaskReference task;



    public TaskContext addNewTaskListener(Consumer<Job> listener) {
        newTaskListeners.add(listener);
        return this;
    }

    /**
     * Initialize a new task context
     *  @param scheduler        The scheduler
     * @param path          The resource path
     * @param workingDirectory The working directory
     * @param logger           The logger
     * @param simulate         Whether to simulate
     * @param newTaskListeners
     */
    public TaskContext(Scheduler scheduler, Long experimentId, Path path, Path workingDirectory, Logger logger,
                       boolean simulate, ArrayList<Consumer<Job>> newTaskListeners) {
        this.scheduler = scheduler;
        this.experimentId = experimentId;
        this.path = path;
        this.workingDirectory = workingDirectory;
        this.logger = logger;
        this.simulate = simulate;
        if (newTaskListeners != null) {
            this.newTaskListeners = newTaskListeners;
        }
    }

    @Override
    public TaskContext clone() {
        return new TaskContext(scheduler, experimentId, path, workingDirectory, logger, simulate, newTaskListeners);
    }

    public boolean simulate() {
        return simulate;
    }

    public Map<Resource, String> defaultLocks() {
        return defaultLocks;
    }

    public TaskContext simulate(boolean simulate) {
        this.simulate = simulate;
        return this;
    }

    public Scheduler getScheduler() {
        return scheduler;
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
        return (Logger) logger.getLoggerRepository().getLogger(loggerName);
    }
}
