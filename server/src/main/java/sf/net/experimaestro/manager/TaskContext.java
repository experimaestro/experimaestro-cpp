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
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

import java.util.Map;

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
     * Associated experiment
     */
    private Experiment experiment;

    /**
     * The current task
     */
    private TaskReference task;

    /**
     * Initialize a new task context
     *  @param scheduler        The scheduler
     * @param path          The resource path
     * @param workingDirectory The working directory
     * @param logger           The logger
     * @param simulate         Whether to simulate
     */
    public TaskContext(Scheduler scheduler, Experiment experiment, Path path, Path workingDirectory, Logger logger, boolean simulate) {
        this.scheduler = scheduler;
        this.experiment = experiment;
        this.path = path;
        this.workingDirectory = workingDirectory;
        this.logger = logger;
        this.simulate = simulate;
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

    public Logger getLogger() {
        return logger;
    }

    public void setTask(TaskReference task) {
        this.task = task;
    }

    public TaskReference getTaskReference() {
        return task;
    }
}
