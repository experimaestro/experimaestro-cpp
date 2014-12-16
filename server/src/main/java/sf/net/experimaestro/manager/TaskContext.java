/*
 * This file is part of experimaestro.
 * Copyright (c) 2013 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.manager;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.vfs2.FileObject;
import sf.net.experimaestro.scheduler.*;
import sf.net.experimaestro.utils.log.Logger;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The context for a running task
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
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
    public FileObject workingDirectory;
    /**
     * The scheduler
     */
    private Scheduler scheduler;
    /**
     * Whether we should simulate
     */
    private boolean simulate;
    /**
     * Resource locator
     */
    private ResourceLocator locator;
    /**
     * List of listeners for new jobs
     */
    ArrayList<Consumer<Job>> newTaskListeners = new ArrayList<>();

    /**
     * Constructs a new task context
     * @param scheduler
     * @param locator
     * @param workingDirectory
     * @param logger
     */


    public TaskContext(Scheduler scheduler, ResourceLocator locator, FileObject workingDirectory, Logger logger) {
        this(scheduler, locator, workingDirectory, logger, false, new ArrayList<>());
    }

    public TaskContext addNewTaskListener(Consumer<Job> listener) {
        newTaskListeners.add(listener);
        return this;
    }

    /**
     * Initialize a new task context
     *  @param scheduler        The scheduler
     * @param locator          The resource locator
     * @param workingDirectory The working directory
     * @param logger           The logger
     * @param simulate         Whether to simulate
     * @param newTaskListeners
     */
    public TaskContext(Scheduler scheduler, ResourceLocator locator, FileObject workingDirectory, Logger logger, boolean simulate, ArrayList<Consumer<Job>> newTaskListeners) {
        this.scheduler = scheduler;
        this.locator = locator;
        this.workingDirectory = workingDirectory;
        this.logger = logger;
        this.simulate = simulate;
        this.newTaskListeners = newTaskListeners;
    }

    @Override
    public TaskContext clone() {
        return new TaskContext(scheduler, locator, workingDirectory, logger, simulate, newTaskListeners);
    }

    public boolean simulate() {
        return simulate;
    }

    public TaskContext defaultLocks(Map<Resource, String> defaultLocks) {
        this.defaultLocks = defaultLocks;
        return this;
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
