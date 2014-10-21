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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import sf.net.experimaestro.connectors.XPMProcess;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.FileNameTransformer;
import sf.net.experimaestro.utils.HeapElement;
import sf.net.experimaestro.utils.ProcessUtils;
import sf.net.experimaestro.utils.Time;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
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
    long startTimestamp;

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

        setState(ResourceState.WAITING);
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
        if (!getState().isActive()) {
            // Set state to waiting
            setState(ResourceState.WAITING);
            clean();

            // Update status
            updateStatus(false);
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
     * @throws Throwable If something goes wrong <b>before</b> starting the process. Otherwise, it should
     *                   return the process
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
            // We are running (prevents other task to try to replace ourselves)
            setState(ResourceState.LOCKING);
            LOGGER.debug("Running preparation - locking ourselves [%s]", this);

            while (true) {
                // Check if not done
                if (isDone()) {
                    setState(ResourceState.DONE);
                    storeState(true);
                    LOGGER.info("Task %s is already done", this);
                    return;
                }

                // Try to lock, otherwise wait
                try {
                    locks.add(getMainConnector().createLockFile(getLocator().path + LOCK_EXTENSION, false));
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

                LOGGER.debug("Running preparation - locked ourselves [%s]", this);

                // Check if not done (again, but now we have a lock so we
                // will be sure of the result)
                if (isDone()) {
                    setState(ResourceState.DONE);
                    storeState(true);
                    LOGGER.info("Task %s is already done", this);
                    return;
                }

                String pid = String.valueOf(ProcessUtils.getPID());

                // Now, tries to lock all the resources
                // in order to avoid race issues, we sync with
                // the task manager
                synchronized (Scheduler.LockSync) {
                    LOGGER.debug("Running preparation - locking dependencies [%s]", this);
                    for (Dependency dependency : getDependencies()) {
                        try {
                            LOGGER.debug("Running preparation - locking dependency [%s]", dependency);
                            final Lock lock = dependency.lock(scheduler, null, pid);
                            if (lock != null)
                                locks.add(lock);
                            LOGGER.debug("Running preparation - locked dependencies [%s]", dependency);
                        } catch (LockException e) {
                            // Update & store this dependency
                            Resource resource = scheduler.getResource(dependency.getFrom());
                            resource.init(scheduler);
                            e.addContext("While locking to run %s", resource);
                            throw e;
                        }

                    }
                }

                // And run!
                LOGGER.info("Locks are OK. Running task [%s]", this);
                try {
                    // Change the state
                    setState(ResourceState.RUNNING);
                    startTimestamp = System.currentTimeMillis();

                    // Start the task and transfer locking handling to those
                    storeState(false);
                    process = startJob(locks);

                    process.adopt(locks);
                    locks = null;

                    // Store the current state
                    storeState(false);

                    LOGGER.info("Task [%s] is running (start=%d) with PID [%s]", this, startTimestamp, process.getPID());

                } catch (Throwable e) {
                    LOGGER.warn(format("Error while running: %s", this), e);
                    setState(ResourceState.ERROR);
                    storeState(true);
                } finally {
                }

                break;
            }
        } catch (LockException e) {
            throw e;
        } catch (RuntimeException e) {
            LOGGER.error(e, "Caught exception for %s", this);
            throw new RuntimeException(e);
        } finally {
            // Dispose of the locks that we own
            if (locks != null)
                for (Lock lock : locks)
                    try {
                        lock.close();
                    } catch (Throwable e) {
                        LOGGER.error(e, "Could not close lock %s", lock);
                    }
        }

    }

    /**
     * Called when a resource state has changed
     *
     * @param message The message
     */
    @Override
    synchronized public void notify(Message message) {
        LOGGER.debug("Notification [%s] for job [%s]", message, this);

        switch (message.getType()) {
            case RESOURCE_REMOVED:
                clean();
                break;

            case END_OF_JOB:
                EndOfJobMessage eoj = (EndOfJobMessage) message;
                this.endTimestamp = eoj.timestamp;

                // TODO: copy done & code to main connector if needed

                LOGGER.info("Job [%s] has ended with code %d", this.getLocator(), eoj.code);

                // Update state
                setState(eoj.code == 0 ? ResourceState.DONE : ResourceState.ERROR);
                // Dispose of the job monitor
                XPMProcess old = process;
                process = null;

                storeState(true);

                try {
                    final Collection<Dependency> deps = getDependencies();
                    for (Dependency dep : deps) {
                        dep.unactivate();

                        scheduler.getResources().store(dep);
                    }
                } catch (RuntimeException e) {
                    LOGGER.error(e, "Error while unactivating dependencies");
                }

                try {
                    LOGGER.debug("Disposing of old XPM process [%s]", old);
                    if (old != null) {
                        old.dispose();
                    }
                } catch (Exception e) {
                    LOGGER.error("Could not dispose of the old process checker %s", e);
                }
                break;

            case DEPENDENCY_CHANGED:
                LOGGER.debug("[before] Locks for job %s: unsatisfied=%d, holding=%d", this, nbUnsatisfied, nbHolding);
                ResourceState oldState = getState();
                final DependencyChangedMessage depChanged = (DependencyChangedMessage) message;

                // Store in cache
                Dependency cachedValue = updateDependency(depChanged.dependency);

                // Use the cached value if we have one since it is most actual
                DependencyStatus fromStatus = cachedValue == null ? depChanged.from : cachedValue.status;

                int diff = (depChanged.to.isOK() ? 1 : 0) - (fromStatus.isOK() ? 1 : 0);
                int diffHold = (depChanged.to.isBlocking() ? 1 : 0) - (fromStatus.isBlocking() ? 1 : 0);

                if (diff != 0 || diffHold != 0) {
                    nbUnsatisfied -= diff;
                    nbHolding += diffHold;

                    // Change the state in funciton of the number of unsatified requirements
                    if (nbUnsatisfied == 0) {
                        setState(ResourceState.READY);
                    } else {
                        if (nbHolding > 0)
                            setState(ResourceState.ON_HOLD);
                        else
                            setState(ResourceState.WAITING);
                    }

                    // Store the result
                    assert nbHolding >= 0;
                    assert nbUnsatisfied >= nbHolding;
                    storeState(getState() != oldState);
                }
                LOGGER.debug("[after] Locks for job %s: unsatisfied=%d, holding=%d [%d/%d] in %s -> %s", this, nbUnsatisfied, nbHolding,
                        diff, diffHold, depChanged.from, depChanged.to);
                break;

            default:
                LOGGER.error("Received unknown self-message: %s", message);

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
    public JSONObject toJSON() throws IOException {
        JSONObject info = super.toJSON();

        if (getState() == ResourceState.DONE
                || getState() == ResourceState.ERROR
                || getState() == ResourceState.RUNNING) {
            long start = getStartTimestamp();
            long end = getState() == ResourceState.RUNNING ? System
                    .currentTimeMillis() : getEndTimestamp();

            JSONObject events = new JSONObject();
            info.put("events", events);

            events.put("start", longDateFormat.format(new Date(start)));

            if (getState() != ResourceState.RUNNING && end >= 0) {
                events.put("end", longDateFormat.format(new Date(end)));
                if (process != null)
                    events.put("pid", process.getPID());
            }
        }

        if (!getDependencies().isEmpty()) {
            JSONArray dependencies = new JSONArray();
            info.put("dependencies", dependencies);

            for (Dependency dependency : getDependencies()) {

                Resource resource = scheduler.getResource(dependency.getFrom());
                resource.init(scheduler);

                JSONObject dep = new JSONObject();
                dependencies.add(dep);
                dep.put("from", resource.getLocator().toString());
                dep.put("fromId", resource.getId());
                dep.put("status", dependency.toString());
            }
        }

        return info;
    }

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
            out.format("<div>%d unsatisfied / %d holding dependencie(s)</div>",
                    nbUnsatisfied, nbHolding);
            for (Dependency dependency : getDependencies()) {

                Resource resource = scheduler.getResource(dependency.getFrom());
                resource.init(scheduler);

                out.format(
                        "<li><a href=\"%s/resource/%d\">%s</a>: %s</li>",
                        config.detailURL,
                        resource.getId(),
                        resource.getLocator(),
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
        // FIXME: handle the case where a dependency is overwritten
        final Dependency old = getDependencyMap().put(dependency.getFrom(), dependency);
        if (old != null) {
            LOGGER.warn("Overwritten dependency: %s", dependency);
        }
    }

    @Override
    synchronized protected boolean doUpdateStatus(boolean store) throws Exception {
        LOGGER.debug("Updating status for [%s]", this);
        boolean changes = super.doUpdateStatus(store);

        // Check the done file
        final FileObject doneFile = getMainConnector().resolveFile(getLocator().getPath() + DONE_EXTENSION);
        if (doneFile.exists() && getState() != ResourceState.DONE) {
            changes = true;
            if (this instanceof Job) {
                ((Job) this).endTimestamp = doneFile.getContent().getLastModifiedTime();
            }
            this.setState(ResourceState.DONE);
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

            LOGGER.debug("After update, state is %s [unsatisfied=%d, holding=%d]", state, nbUnsatisfied, nbHolding);
            changes |= setState(state);
        }

        return changes;
    }

    /**
     * Stop the job
     */
    synchronized public boolean stop() {
        // Process is running
        if (process != null) {
            try {
                process.destroy();
            } catch (FileSystemException e) {
                LOGGER.error(e, "The process could not be stopped");
                return false;
            }
            setState(ResourceState.ERROR);
            storeState(true);
            return true;
        }

        // Process is about to run
        if (getState() == ResourceState.READY || getState() == ResourceState.WAITING) {
            setState(ResourceState.ON_HOLD);
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

    /**
     * Remove a file linked to this job
     */
    private void removeJobFile(FileNameTransformer t) {
        try (final FileObject file = t.transform(getMainConnector().resolveFile(getLocator().getPath()))) {
            if (file.exists())
                file.delete();
        } catch (FileSystemException e) {
            LOGGER.info(e, "Could not remove '%s' file: %s / %s", getLocator(), t);
        }
    }


    @Override
    public ReadWriteDependency createDependency(Object object) {
        // TODO: assert object is nothing
        return new ReadWriteDependency(getId());
    }
}
