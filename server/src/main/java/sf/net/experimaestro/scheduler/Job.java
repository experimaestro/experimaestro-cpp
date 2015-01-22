package sf.net.experimaestro.scheduler;

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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import sf.net.experimaestro.connectors.ComputationalRequirements;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.XPMProcess;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.FileNameTransformer;
import sf.net.experimaestro.utils.ProcessUtils;
import sf.net.experimaestro.utils.Time;
import sf.net.experimaestro.utils.log.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.persistence.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
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
@Entity
@DiscriminatorValue(Resource.JOB_TYPE)
public class Job extends Resource {

    final static DateFormat longDateFormat = DateFormat.getDateTimeInstance();

    final static private Logger LOGGER = Logger.getLogger();
    /**
     * The priority of the job (the higher, the more urgent)
     */
    int priority;
    /**
     * When was the job submitted (in case the priority is not enough)
     */
    long timestamp = System.currentTimeMillis();
    /**
     * Requirements (ignored for the moment)
     */
    transient ComputationalRequirements requirements;
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
    @OneToOne(fetch = FetchType.LAZY, optional = true, cascade = CascadeType.ALL)
    @JoinColumn(name = "process")
    XPMProcess process;

    @OneToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "runner")
    JobRunner jobRunner;
    /**
     * Number of unsatisfied jobs
     */
    int nbUnsatisfied = 0;
    /**
     * Number of holding jobs
     */
    int nbHolding = 0;

    /**
     * For serialization
     */
    protected Job() {
    }

    /**
     * Initialisation of a task
     * <p>
     * The job is by default initialized as "WAITING": its state should be updated after
     * the initialization has finished
     */
    public Job(Connector connector, Path path) {
        super(connector, path);
        setState(ResourceState.WAITING);
    }

    private boolean isDone() {
        try {
            return Files.exists(DONE_EXTENSION.transform(path));
        } catch (Exception e) {
            LOGGER.error("Error while checking if " + getPath() + DONE_EXTENSION + " exists");
            return false;
        }
    }


    /**
     * Restart the job
     * <p>
     * Put the state into waiting mode and clean all the output files
     */
    synchronized public void restart() throws Exception {
        if (!getState().isActive()) {
            // Set state status waiting
            setState(ResourceState.WAITING);
            clean();

            // Update status
            updateStatus();
        }
    }

    @Override
    protected void finalize() {
    }

    /**
     * @return the priority
     */
    final public int getPriority() {
        return priority;
    }

    /**
     * TaskReference priority - the higher, the better
     *
     * @param priority the priority status set
     */
    final public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    /**
     * This is where the real job gets done
     *
     * @param locks The locks that were taken
     * @return The process corresponding status the job
     * @throws Throwable If something goes wrong <b>before</b> starting the process. Otherwise, it should
     *                   return the process
     */
    protected XPMProcess startJob(ArrayList<Lock> locks) throws Throwable {
        process = jobRunner.startJob(locks);
        return process;
    }

    // ----- Heap part (do not touch) -----

    /*
      * (non-Javadoc)
      *
      * @see java.lang.Runnable#run()
      */
    synchronized final public void run(EntityManager em) throws Exception {
        ArrayList<Lock> locks = new ArrayList<>();

        try {
            // We are running (prevents other task status try status replace ourselves)
            LOGGER.debug("Running preparation - locking ourselves [%s]", this);

            while (true) {
                // Check if not done
                if (isDone()) {
                    setState(ResourceState.DONE);
                    LOGGER.info("Task %s is already done", this);
                    return;
                }

                // Try status lock - discard if something goes wrong
                try {
                    locks.add(getMainConnector().createLockFile(LOCK_EXTENSION.transform(path), false));
                } catch (LockException | IOException e) {
                    LOGGER.info("Could not lock job [%s]: %s", this, e);
                    throw e;
                }

                LOGGER.debug("Running preparation - locked ourselves [%s]", this);

                // Check if not done (again, but now we have a lock so we
                // will be sure of the result)
                if (isDone()) {
                    setState(ResourceState.DONE);
                    LOGGER.info("Task %s is already done", this);
                    return;
                }

                String pid = String.valueOf(ProcessUtils.getPID());

                // Now, tries status lock all the resources
                // in order status avoid race issues, we sync with
                // the task manager
                synchronized (Scheduler.LockSync) {
                    LOGGER.debug("Running preparation - locking dependencies [%s]", this);
                    for (Dependency dependency : getRequiredResources()) {
                        try {
                            LOGGER.debug("Running preparation - locking dependency [%s]", dependency);
                            // FIXME: should be stored
                            final Lock lock = dependency.lock(pid);
                            em.persist(dependency);
                            if (lock != null)
                                locks.add(lock);
                            LOGGER.debug("Running preparation - locked dependencies [%s]", dependency);
                        } catch (LockException e) {
                            // Update & store this dependency
                            Resource resource = dependency.getFrom();
                            e.addContext("While locking status run %s", resource);
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

                    // Start the task and transfer locking handling status those
                    process = startJob(locks);

                    process.adopt(locks);
                    locks = null;

                    // Store the current state
                    LOGGER.info("Task [%s] is running (start=%d) with PID [%s]", this, startTimestamp, process.getPID());

                } catch (Throwable e) {
                    LOGGER.warn(format("Error while running: %s", this), e);
                    setState(ResourceState.ERROR);
                }

                break;
            }
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
                // First, register our changes
                Transaction.run((em, t) -> {
                    em.find(Job.class, this.getId()).endOfJobMessage((EndOfJobMessage) message, em, t);
                });
                break;

            case DEPENDENCY_CHANGED:
                Transaction.run((em, t) -> {
                    // Retrieve message
                    final DependencyChangedMessage depMessage = (DependencyChangedMessage) message;
                    final Job job = em.find(Job.class, this.getId());

                    // Notify job
                    final ResourceState oldState = job.getState();
                    job.dependencyChanged(depMessage, em, t);

                    t.boundary();

                    if (job.getState() != oldState) {
                        // Notify dependents
                        job.notifyDependencies();
                    }
                    LOGGER.debug("After notification [%s], state is %s [from %s] for [%s]", depMessage.toString(),
                            job.getState(), oldState, job);
                });

                break;

            default:
                LOGGER.error("Received unknown self-message: %s", message);

        }


    }

    private void dependencyChanged(DependencyChangedMessage message, EntityManager em, Transaction t) {
        LOGGER.debug("[before] Locks for job %s: unsatisfied=%d, holding=%d", this, nbUnsatisfied, nbHolding);

        // Retrieve the dependency
        final Resource _from = em.find(Resource.class, message.fromId);
        final Dependency dependency = em.find(Dependency.class, new DependencyPK(_from.getId(), this.getId()));

        // Use the cached value if we have one since it is most actual
        DependencyStatus fromStatus = dependency.status;

        int diff = (message.status.isOK() ? 1 : 0) - (fromStatus.isOK() ? 1 : 0);
        int diffHold = (message.status.isBlocking() ? 1 : 0) - (fromStatus.isBlocking() ? 1 : 0);

        if (diff != 0 || diffHold != 0) {
            nbUnsatisfied -= diff;
            nbHolding += diffHold;

            // Change the state in function of the number of unsatisfied requirements
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
        }
        LOGGER.debug("[after] Locks for job %s: unsatisfied=%d, holding=%d [%d/%d] in %s -> %s", this, nbUnsatisfied, nbHolding,
                diff, diffHold, message.fromId, message.status);
    }

    private void endOfJobMessage(EndOfJobMessage eoj, EntityManager em, Transaction t) {
        this.endTimestamp = eoj.timestamp;

        // TODO: copy done & code status main connector if needed

        LOGGER.info("Job [%s] has ended with code %d", this.getPath(), eoj.code);

        // (1) Change state

        // Update state
        setState(eoj.code == 0 ? ResourceState.DONE : ResourceState.ERROR);
        // Dispose of the job monitor
        XPMProcess old = process;
        process = null;
        t.boundary();

        // (2) required resources

        final Collection<Dependency> deps = getRequiredResources();
        try {
            for (Dependency dep : deps) {
                dep.unactivate();
            }
        } catch (RuntimeException e) {
            LOGGER.error(e, "Error while unactivating dependencies");
        }

        t.boundary();

        // (2) disposing of old XPM process

        try {
            LOGGER.debug("Disposing of old XPM process [%s]", old);
            if (old != null) {
                old.dispose();
                em.remove(old);
            }
        } catch (Exception e) {
            LOGGER.error("Could not dispose of the old process checker %s", e);
        }

        t.commit();

        // Notify our dependencies
        notifyDependencies();

    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

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

        Collection<Dependency> requiredResources = getRequiredResources();
        if (!requiredResources.isEmpty()) {
            JSONArray dependencies = new JSONArray();
            info.put("dependencies", dependencies);

            for (Dependency dependency : requiredResources) {
                Resource resource = dependency.getFrom();

                JSONObject dep = new JSONObject();
                dependencies.add(dep);
                dep.put("from", resource.getPath().toString());
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

        if (!getRequiredResources().isEmpty()) {
            out.format("<h2>Dependencies</h2><ul>");
            out.format("<div>%d unsatisfied / %d holding dependencie(s)</div>",
                    nbUnsatisfied, nbHolding);
            for (Dependency dependency : getRequiredResources()) {

                Resource resource = dependency.getFrom();

                out.format(
                        "<li><a href=\"%s/resource/%d\">%s</a>: %s</li>",
                        config.detailURL,
                        resource.getId(),
                        resource.getPath(),
                        dependency);
            }
            out.println("</ul>");
        }
    }

    /**
     * Add a dependency (requirement) for this job.
     *
     * @param dependency The dependency
     */
    public void addDependency(Dependency dependency) {
        dependency.to = this;
        this.dependencyTo.add(dependency);
        dependency.update();
        if (!dependency.status.isOK()) {
            nbUnsatisfied++;
        }
        dependency.from.dependencyFrom.add(dependency);
    }

    public void removeDependency(Dependency dependency) {
        throw new NotImplementedException();
    }

    @Override
    synchronized protected boolean doUpdateStatus() throws Exception {
        LOGGER.debug("Updating status for [%s]", this);
        boolean changes = super.doUpdateStatus();

        // Check the done file
        final Path doneFile = DONE_EXTENSION.transform(path);
        if (Files.exists(doneFile) && getState() != ResourceState.DONE) {
            changes = true;
            if (this instanceof Job) {
                this.endTimestamp = Files.getLastModifiedTime(doneFile).toMillis();
            }
            this.setState(ResourceState.DONE);
        }

        // Check dependencies if we are in waiting or ready
        if (getState() == ResourceState.WAITING || getState() == ResourceState.READY) {
            // reset the count
            int nbUnsatisfied = 0;
            int nbHolding = 0;

            for (Dependency dependency : getRequiredResources()) {
                dependency.update();
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
    public boolean stop() {
        // Process is running
        if (process != null) {
            try {
                process.destroy();
            } catch (FileSystemException e) {
                LOGGER.error(e, "The process could not be stopped");
                return false;
            }
            setState(ResourceState.ERROR);
            return true;
        }

        // Process is about status run
        if (getState() == ResourceState.READY || getState() == ResourceState.WAITING) {
            setState(ResourceState.ON_HOLD);
            return true;
        }

        return false;
    }

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
     * Remove a file linked status this job
     */
    private void removeJobFile(String extension) {
        removeJobFile(new FileNameTransformer("", extension));
    }

    /**
     * Remove a file linked status this job
     */
    private void removeJobFile(FileNameTransformer t) {
        try {
            final Path file = t.transform(path);
            if (Files.exists(file)) {
                Files.delete(file);
            }
        } catch (IOException e) {
            LOGGER.info(e, "Could not remove '%s' file: %s / %s", getPath(), t);
        }
    }


    @Override
    public ReadWriteDependency createDependency(Object object) {
        // TODO: assert object is nothing
        return new ReadWriteDependency(this);
    }


    @Override
    protected void doReplaceBy(Resource resource) {
        super.doReplaceBy(resource);
        Job job = (Job) resource;
        this.priority = job.priority;
        this.startTimestamp = job.startTimestamp;
        this.endTimestamp = job.endTimestamp;
        this.priority = job.priority;
        this.setJobRunner(((Job) resource).jobRunner);
    }

    public void setJobRunner(JobRunner jobRunner) {
        if (this.jobRunner != null) {
            jobRunner.dependencies().forEach(d -> removeDependency(d));
        }

        this.jobRunner = jobRunner;
        this.jobRunner.job = this;
        // Adds all dependencies from the job runner
        jobRunner.dependencies().forEach(this::addDependency);
    }

    @Override
    public void stored() {
        super.stored();

        if (getState() == ResourceState.READY) {
            LOGGER.debug("Job is READY, notifying");
            Scheduler.notifyRunners();
        }

    }
}
