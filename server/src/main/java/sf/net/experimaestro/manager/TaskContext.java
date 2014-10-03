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
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.ResourceLocator;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

import java.util.Map;

/**
 * The context for a running task
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TaskContext {
    /**
     * The scheduler
     */
    private Scheduler scheduler;

    /**
     * Whether we should simulate
     */
    private boolean simulate;

    /**
     * The default locks
     */
    public Map<Resource, String> defaultLocks = ImmutableMap.of();

    /**
     * Resource locator
     */
    private ResourceLocator locator;

    /**
     * The working directory
     */
    public FileObject workingDirectory;

    /**
     * The logger
     */
    private final Logger logger;


    public TaskContext(Scheduler scheduler, ResourceLocator locator, FileObject workingDirectory, Logger logger) {
        this(scheduler, locator, workingDirectory, logger, false);
    }

    /**
     * Initialize a new task context
     * @param scheduler The scheduler
     * @param locator The resource locator
     * @param workingDirectory The working directory
     * @param logger The logger
     * @param simulate Whether to simulate
     */
    public TaskContext(Scheduler scheduler, ResourceLocator locator, FileObject workingDirectory, Logger logger, boolean simulate) {
        this.scheduler = scheduler;
        this.locator = locator;
        this.workingDirectory = workingDirectory;
        this.logger = logger;
        this.simulate = simulate;
    }

    @Override
    public TaskContext clone() {
        return new TaskContext(scheduler, locator, workingDirectory, logger, simulate);
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
}
