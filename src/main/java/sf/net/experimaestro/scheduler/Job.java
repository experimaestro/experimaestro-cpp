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

package sf.net.experimaestro.scheduler;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.Persistent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.connectors.ComputationalRequirements;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.XPMProcess;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.UnlockableException;
import sf.net.experimaestro.server.XPMServlet;
import sf.net.experimaestro.utils.HeapElement;
import sf.net.experimaestro.utils.ProcessUtils;
import sf.net.experimaestro.utils.Time;
import sf.net.experimaestro.utils.log.Logger;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static java.lang.String.format;

/**
 * A job is a resource that can be run - that starts and ends (which
 * differentiate it with a server) and generate data
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Persistent()
public abstract class Job extends Resource implements HeapElement<Job> {

    final static private Logger LOGGER = Logger.getLogger();

    protected Job() {
    }

    /**
     * Initialisation of a task
     * <p/>
     * The job is by default initialized as "WAITING": its state should be updated after
     * the initialization has finished
     *
     * @param scheduler The job scheduler
     */
    public Job(Scheduler scheduler, Connector connector, String identifier) {
        super(scheduler, connector, identifier, LockMode.EXCLUSIVE_WRITER);
        // State is on hold for the moment
        state = ResourceState.ON_HOLD;
    }

    private boolean isDone() {
        try {
            return getMainConnector().resolveFile(locator.path + DONE_EXTENSION).exists();
        } catch (Exception e) {
            LOGGER.error("Error while checking if " + locator + DONE_EXTENSION + " exists");
            return false;
        }
    }

    /**
     * The priority of the job (the higher, the more urgent)
     */
    int priority;

    /**
     * When was the job submitted (in case the priority is not enough)
     */
    long timestamp = System.currentTimeMillis();

    /**
     * When did the job start (0 if not started)
     */
    private long startTimestamp;

    /**
     * When did the job stop (0 when it did not stop yet)
     */
    long endTimestamp;

    /**
     * Our job monitor
     */
    XPMProcess process;

    /**
     * Requirements
     */
    ComputationalRequirements requirements;


    @Override
    protected void finalize() {
    }

    /**
     * Task priority - the higher, the better
     *
     * @param priority the priority to set
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * This is where the real job gets done
     *
     * @param locks The locks that were taken
     * @return The process corresponding to the job
     * @throws Throwable If something goes wrong
     */
    abstract protected XPMProcess startJob(ArrayList<Lock> locks) throws Throwable;

    /**
     * Initialize the object when retrieved from database
     *
     * @param scheduler
     * @throws DatabaseException
     */
    @Override
    public boolean init(Scheduler scheduler) throws DatabaseException {
        if (!super.init(scheduler))
            return false;

        if (process != null)
            process.init(this);

        return true;
    }

    /*
      * (non-Javadoc)
      *
      * @see java.lang.Runnable#run()
      */
    synchronized final public void run() throws LockException {
        ArrayList<Lock> locks = new ArrayList<>();

        try {
            while (true) {
                // Check if not done
                if (isDone()) {
                    state = ResourceState.DONE;
                    updateDb(null, Changes.STATE);
                    LOGGER.info("Task %s is already done", locator);
                    return;
                }

                // Try to lock, otherwise wait
                try {
                    locks.add(getMainConnector().createLockFile(locator.path + LOCK_EXTENSION));
                } catch (UnlockableException e) {
                    LOGGER.info("Could not lock job [%s]", locator);
                    synchronized (this) {
                        try {
                            // Wait five seconds before trying to lock again
                            wait(5000);
                        } catch (InterruptedException ee) {
                        }
                    }
                    continue;
                }

                // Check if not done (again, but now we have a lock so we
                // will be sure of the result)
                if (isDone()) {
                    updateDb(null, Changes.STATE);
                    LOGGER.info("Task %s is already done", locator);
                    return;
                }

                String pid = String.valueOf(ProcessUtils.getPID());

                // Now, tries to lock all the resources
                // in order to avoid race issues, we sync with
                // the task manager
                synchronized (Scheduler.LockSync) {
                    for (Dependency dependency : getDependencies()) {
                        ResourceLocator id = dependency.getFrom();
                        Resource rsrc = scheduler.getResource(id);
                        final Lock lock = rsrc.lock(pid, dependency.type);
                        if (lock != null)
                            locks.add(lock);
                        else {
                            throw new LockException("Could not lock %s", id);
                        }

                    }
                }

                // And run!
                LOGGER.info("Locks are OK. Running task [%s]", locator);
                try {
                    // Change the state
                    state = ResourceState.RUNNING;
                    startTimestamp = System.currentTimeMillis();

                    // Start the task and transfer locking handling to those
                    process = startJob(locks);
                    process.adopt(locks);
                    locks = null;
                    updateDb(null);

                } catch (Throwable e) {
                    LOGGER.warn(format("Error while running: %s", this), e);
                    state = ResourceState.ERROR;
                } finally {
                }

                break;
            }
        } catch (UnlockableException e) {
            throw new RuntimeException(e);
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        } catch (LockException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // Dispose of the locks that we own
            if (locks != null)
                for (Lock lock : locks)
                    try {
                        lock.dispose();
                    } catch (Exception e) {
                        LOGGER.error(e);
                    }
            try {
                if (process != null && !process.isRunning())
                    process.dispose();
            } catch (Exception e) {
                LOGGER.error("Error while disposing of locks of XPMProcess: %s", e);
            }
        }

    }

    /**
     * Called when a resource status has changed
     *
     * @param resource The resource has changed (or null if itself)
     * @param objects  Optional parameters
     */
    @Override
    synchronized public void notify(Resource resource, Object... objects) {
        LOGGER.debug("Notification [%s] for job [%s]", Arrays.toString(objects), resource);

        // Self-notification
        if (resource == null || resource == this) {
            // Notified of the end of job
            if (objects.length == 1 && objects[0] instanceof EndOfJobMessage) {
                EndOfJobMessage message = (EndOfJobMessage) objects[0];
                this.endTimestamp = message.timestamp;

                // TODO: copy done & code to main connector if needed

                LOGGER.info("Job [%s] has ended with code %d", this.getLocator(), message.code);

                // Update state
                state = message.code == 0 ? ResourceState.DONE : ResourceState.ERROR;
                // Dispose of the job monitor
                XPMProcess old = process;
                process = null;

                try {
                    scheduler.store(this, Changes.STATE);
                } catch (ExperimaestroCannotOverwrite experimaestroCannotOverwrite) {
                    LOGGER.error("Cannot overwrite ourselves... strange!");
                }
                try {
                    LOGGER.debug("Disposing of old XPM process [%s]", old);
                    if (old != null) {
                        old.dispose();
                    }
                } catch (Exception e) {
                    LOGGER.error("Could not dispose of the old process checker %s", e);
                }
            } else if (objects.length == 1 && objects[0] instanceof SimpleMessage) {
                switch ((SimpleMessage) objects[0]) {
                    case STORED_IN_DATABASE:
                        break;
                    default:
                        LOGGER.error("Received unknown self-message: %s", objects[0]);
                }
            } else {
                LOGGER.error("Received unknown self-message: %s", Arrays.toString(objects));
            }
        } else {
            Changes changes = new Changes();
            checkDependency(resource, false, changes);
            if (changes.changed())
                try {
                    scheduler.store(null, this, changes);
                } catch (ExperimaestroCannotOverwrite experimaestroCannotOverwrite) {
                     LOGGER.error("Could not overwrite ourselves!");
                }
        }

    }


    // ----- Heap part (do not touch) -----

    /**
     * Negative value when not in the heap. It should
     * not be serialized since it is linked to a list of jobs
     * that should remain in main memory
     */
    transient private int index = -1;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    // ----- [/Heap part] -----

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    final static DateFormat longDateFormat = DateFormat.getDateTimeInstance();

    @Override
    public void printXML(PrintWriter out, PrintConfig config) {
        super.printXML(out, config);

        out.format("<h2>Locking status</h2>%n");
        out.format("<div><b>Lock</b>: %s</div>", isLocked() ? "Locked"
                : "Not locked");
        out.format("<div>%d writer(s) and %d reader(s)</div>", getReaders(),
                getWriters());

        if (getState() == ResourceState.DONE
                || getState() == ResourceState.ERROR
                || getState() == ResourceState.RUNNING) {
            long start = getStartTimestamp();
            long end = getState() == ResourceState.RUNNING ? System
                    .currentTimeMillis() : getEndTimestamp();

            out.format("<div>Started: %s</div>",
                    longDateFormat.format(new Date(start)));

            if (getState() != ResourceState.RUNNING && end >= 0) {
                out.format("<div>Ended: %s</div>",
                        longDateFormat.format(new Date(end)));
                out.format("<div>Duration: %s</div>",
                        Time.formatTimeInMilliseconds(end - start));
                if (process != null)
                    out.format("<div>PID: %s</div>", process.getPID());
            }
        }

        if (!getDependencies().isEmpty()) {
            out.format("<h2>Dependencies</h2><ul>");
            out.format("<div>%d (%d) unsatisfied (holding) dependencie(s)</div>",
                    nbUnsatisfied, nbHolding);
            for (Dependency dependency : getDependencies()) {
                ResourceLocator locator = dependency.getFrom();
                Resource resource = null;
                try {
                    resource = scheduler.getResource(locator);
                } catch (DatabaseException e) {
                }
                out.format(
                        "<li><a href=\"%s/resource?id=%s&amp;path=%s\">%s</a>: %s [%b/%s/%b]</li>",
                        config.detailURL,
                        XPMServlet.urlEncode(locator.getConnectorId()),
                        XPMServlet.urlEncode(locator.getPath()),
                        locator,
                        dependency.getType(),
                        dependency.isSatisfied,
                        dependency.state,
                        resource != null && resource.accept(dependency.type).isOK()
                );
            }
            out.println("</ul>");
        }
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    synchronized protected void doUpdateStatus(Changes changes) throws Exception {
        super.doUpdateStatus(changes);

        // Check dependencies if we are in waiting or ready
        if (getState() == ResourceState.WAITING || getState() == ResourceState.READY) {
            // reset the count
            nbUnsatisfied = 0;
            nbHolding = 0;
            ResourceState old = state;

            // TODO: smarter check (could say something has changed when nothing has)
            this.state = ResourceState.READY;
            for (Dependency dependency : getDependencies()) {
                checkDependency(scheduler.getResource(dependency.getFrom()), true, changes);
            }
            changes.state = old != state;


        }
    }

    /**
     * Stop the job
     */
    synchronized public boolean stop() {
        // Process is running
        if (process != null) {
            process.destroy();
            state = ResourceState.ERROR;
            updateDb(Changes.STATE);
            return true;
        }

        // Process is about to run
        if (state == ResourceState.READY || state == ResourceState.WAITING) {
            state = ResourceState.ON_HOLD;
            updateDb(Changes.STATE);
            return true;
        }

        return false;
    }

    @Override
    public void clean() {
        super.clean();
        LOGGER.info("Cleaning job %s", this);
        removeJobFile(DONE_EXTENSION);
        removeJobFile(CODE_EXTENSION);
        removeJobFile(ERR_EXTENSION);
        removeJobFile(OUT_EXTENSION);
        removeJobFile(RUN_EXTENSION);
    }

    /** Remove a file linked to this job */
    private void removeJobFile(String extension) {
        try (final FileObject doneFile = getMainConnector().resolveFile(locator.getPath() + extension)) {
            if (doneFile.exists())
                doneFile.delete();
        } catch (FileSystemException e) {
            LOGGER.info("Could not remove '%s' file: %s", extension, e);
        }
    }

}
