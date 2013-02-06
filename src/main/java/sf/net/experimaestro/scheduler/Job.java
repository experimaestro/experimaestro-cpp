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
import sf.net.experimaestro.connectors.XPMProcess;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.HeapElement;
import sf.net.experimaestro.utils.ProcessUtils;
import sf.net.experimaestro.utils.Time;
import sf.net.experimaestro.utils.log.Logger;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import static java.lang.String.format;

/**
 * A job is a resource that can be run - that starts and ends (which
 * differentiate it with a server) and generate data
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Persistent()
public abstract class Job<Data extends JobData> extends Resource<Data> implements HeapElement<Job<? extends JobData>> {
    final static private Logger LOGGER = Logger.getLogger();


    /**
     * When did the job start (0 if not started)
     */
    private long startTimestamp;

    /**
     * When did the job stop (0 when it did not stop yet)
     */
    long endTimestamp;

    /**
     * Our job monitor (null when there is no attached process)
     */
    XPMProcess process;

    /**
     * Number of unsatisfied jobs
     */
    int nbUnsatisfied = 0;

    /**
     * Number of holding jobs
     */
    int nbHolding = 0;


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
    public Job(Scheduler scheduler, Data data) {
        super(scheduler, data);

        // State is on hold for the moment
        state = ResourceState.ON_HOLD;
    }

    private boolean isDone() {
        try {
            return getMainConnector().resolveFile(getLocator().path + DONE_EXTENSION).exists();
        } catch (Exception e) {
            LOGGER.error("Error while checking if " + getLocator() + DONE_EXTENSION + " exists");
            return false;
        }
    }


    /**
     * Restart the job
     * <p/>
     * Put the state into waiting mode and clean all the output files
     */
    synchronized public void restart() throws Exception {
        if (!state.isActive()) {
            set(ResourceState.WAITING);
            clean();
            scheduler.store(this, true);
        }
    }


    @Override
    protected void finalize() {
    }

    /**
     * Task priority - the higher, the better
     *
     * @param priority the priority to set
     */
    final public void setPriority(int priority) {
        getData().priority = priority;
    }

    /**
     * @return the priority
     */
    final public int getPriority() {
        return getData().priority;
    }

    public long getTimestamp() {
        return getData().timestamp;
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
                    storeState(true);
                    LOGGER.info("Task %s is already done", this);
                    return;
                }

                // Try to lock, otherwise wait
                try {
                    locks.add(getMainConnector().createLockFile(getLocator().path + LOCK_EXTENSION));
                } catch (LockException e) {
                    LOGGER.info("Could not lock job [%s]", this);
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
                    storeState(true);
                    LOGGER.info("Task %s is already done", this);
                    return;
                }

                String pid = String.valueOf(ProcessUtils.getPID());

                // Now, tries to lock all the resources
                // in order to avoid race issues, we sync with
                // the task manager
                synchronized (Scheduler.LockSync) {
                    for (Dependency dependency : getDependencies()) {
                        try {
                            final Lock lock = dependency.lock(scheduler, null, pid);
                            if (lock != null)
                                locks.add(lock);
                        } catch (LockException e) {
                            dependency.update(scheduler, null, true);
                            throw new LockException(e, "Could not lock %s", dependency);
                        }

                    }
                }

                // And run!
                LOGGER.info("Locks are OK. Running task [%s]", this);
                try {
                    // Change the state
                    state = ResourceState.RUNNING;
                    startTimestamp = System.currentTimeMillis();

                    // Start the task and transfer locking handling to those
                    process = startJob(locks);
                    process.adopt(locks);
                    locks = null;
                    storeState(false);

                } catch (Throwable e) {
                    LOGGER.warn(format("Error while running: %s", this), e);
                    state = ResourceState.ERROR;
                } finally {
                }

                break;
            }
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
                        lock.close();
                    } catch (Exception e) {
                        LOGGER.error(e);
                    }
        }

    }

    /**
     * Called when a resource state has changed
     *
     * @param message The message
     */
    @Override
    synchronized public void notify(Resource resource, Message message) {
        LOGGER.debug("Notification [%s] for job [%s]", message, this);

        if (resource == this || resource == null) {
            // --- Notified of the end of job
            if (message instanceof EndOfJobMessage) {
                EndOfJobMessage eoj = (EndOfJobMessage) message;
                this.endTimestamp = eoj.timestamp;

                // TODO: copy done & code to main connector if needed

                LOGGER.info("Job [%s] has ended with code %d", this.getLocator(), eoj.code);

                // Update state
                state = eoj.code == 0 ? ResourceState.DONE : ResourceState.ERROR;
                // Dispose of the job monitor
                XPMProcess old = process;
                process = null;

                storeState(true);

                final Collection<Dependency> deps = getDependencies();
                for (Dependency dep : deps) {
                    dep.status = DependencyStatus.UNACTIVE;
                    scheduler.getResources().store(dep);
                }

                try {
                    LOGGER.debug("Disposing of old XPM process [%s]", old);
                    if (old != null) {
                        old.dispose();
                    }
                } catch (Exception e) {
                    LOGGER.error("Could not dispose of the old process checker %s", e);
                }
            } else if (message instanceof SimpleMessage.Wrapper) {
                switch (((SimpleMessage.Wrapper) message).get()) {
                    case STORED_IN_DATABASE:
                        break;
                    default:
                        LOGGER.error("Received unknown self-message: %s", message);
                }
            } else if (message instanceof DependencyChangedMessage) {
                final DependencyChangedMessage depChanged = (DependencyChangedMessage) message;

                // Store in cache
                updateDependency(depChanged.dependency);

                int diff = (depChanged.to.isOK() ? 1 : 0) - (depChanged.from.isOK() ? 1 : 0);
                int diffHold = (depChanged.to.isBlocking() ? 1 : 0) - (depChanged.from.isBlocking() ? 1 : 0);

                if (diff != 0 || diffHold != 0) {
                    nbUnsatisfied -= diff;
                    nbHolding += diffHold;

                    // Manages the holding count
                    if (depChanged.to == DependencyStatus.ERROR) {
                        nbHolding++;
                    } else if (depChanged.from == DependencyStatus.ERROR) {
                        nbHolding--;
                    }


                    // Change the state in funciton of the number of unsatified requirements
                    if (nbUnsatisfied == 0) {
                        if (state == ResourceState.WAITING)
                            state = ResourceState.READY;
                    } else {
                        if (state == ResourceState.READY)
                            state = ResourceState.WAITING;
                    }

                    // Store the result
                    assert nbHolding >= 0;
                    assert nbUnsatisfied >= nbHolding;
                    storeState(false);
                }
                LOGGER.debug("Locks for job %s: unsatisfied=%d, holding=%d", this, nbUnsatisfied, nbHolding);
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

//        getData().printXML();

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
                out.format(
                        "<li><a href=\"%s/resource/%d\">%s</a>: %s</li>",
                        config.detailURL,
                        getId(),
                        getLocator(),
                        dependency);
            }
            out.println("</ul>");
        }
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void addDependency(Dependency dependency) {
        if (stored())
            throw new RuntimeException("Cannot add dependencies to a stored resource");

        // Start with an unsatisfied dependency -- the status will be updated
        // when storing the resource
        dependency.status = DependencyStatus.WAIT;
        nbUnsatisfied++;
        getDependencyMap().put(dependency.getFrom(), dependency);

    }

    @Override
    synchronized protected boolean doUpdateStatus(boolean store) throws Exception {
        boolean changes = super.doUpdateStatus(store);

        // Check the done file
        final FileObject doneFile = getMainConnector().resolveFile(getLocator().getPath() + DONE_EXTENSION);
        if (doneFile.exists() && state != ResourceState.DONE) {
            changes = true;
            if (this instanceof Job) {
                ((Job) this).endTimestamp = doneFile.getContent().getLastModifiedTime();
            }
            this.state = ResourceState.DONE;
        }

        // Check dependencies if we are in waiting or ready
        if (getState() == ResourceState.WAITING || getState() == ResourceState.READY) {
            // reset the count
            int nbUnsatisfied = 0;
            int nbHolding = 0;

            for (Dependency dependency : getDependencies()) {
                dependency.update(scheduler, null, store);
                if (!dependency.status.isOK()) {
                    nbUnsatisfied++;
                    if (dependency.status == DependencyStatus.HOLD)
                        nbHolding++;
                }
            }

            ResourceState state = nbUnsatisfied > 0 ? ResourceState.WAITING : ResourceState.READY;
            if (nbHolding > 0)
                state = ResourceState.ON_HOLD;

            if (nbUnsatisfied != this.nbUnsatisfied) {
                changes = true;
                this.nbUnsatisfied = nbUnsatisfied;
            }

            if (nbHolding != this.nbHolding) {
                changes = true;
                this.nbHolding = nbHolding;
            }

            changes |= set(state);
        }

        return changes;
    }

    /**
     * Stop the job
     */
    synchronized public boolean stop() {
        // Process is running
        if (process != null) {
            process.destroy();
            state = ResourceState.ERROR;
            storeState(true);
            return true;
        }

        // Process is about to run
        if (state == ResourceState.READY || state == ResourceState.WAITING) {
            state = ResourceState.ON_HOLD;
            storeState(true);
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

    /**
     * Remove a file linked to this job
     */
    private void removeJobFile(String extension) {
        try (final FileObject doneFile = getMainConnector().resolveFile(getLocator().getPath() + extension)) {
            if (doneFile.exists())
                doneFile.delete();
        } catch (FileSystemException e) {
            LOGGER.info("Could not remove '%s' file: %s", extension, e);
        }
    }


    @Override
    public ReadWriteDependency createDependency(Object object) {
        // TODO: assert object is nothing
        return new ReadWriteDependency(getId());
    }
}
