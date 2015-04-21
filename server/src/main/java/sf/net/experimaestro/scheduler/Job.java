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

import com.google.common.collect.Iterables;
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
import sf.net.experimaestro.utils.jpa.JobRunnerConverter;
import sf.net.experimaestro.utils.log.Logger;

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
    @Basic(fetch = FetchType.LAZY)
    @OneToOne(optional = true, cascade = CascadeType.ALL, orphanRemoval = true)
    XPMProcess process;

    /**
     * The process
     */

    @Column(name = "jobRunner", columnDefinition = "VARCHAR(128000)")
    @Basic(fetch = FetchType.LAZY)
    String jobRunnerString;

    /**
     * The unserialized job runner
     */
    transient private JobRunner jobRunner;

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
    public Job(Connector connector, Path path) throws IOException {
        super(connector, path);
        setState(ResourceState.WAITING);
    }

    public Job(Connector connector, String path) {
        super(connector, path);
        setState(ResourceState.WAITING);
    }

    private boolean isDone() {
        try {
            return Files.exists(DONE_EXTENSION.transform(getPath()));
        } catch (Exception e) {
            LOGGER.error("Error while checking if " + getLocator() + DONE_EXTENSION + " exists");
            return false;
        }
    }


    /**
     * Restart the job
     * <p>
     * Put the state into waiting mode and clean all the output files
     */
    synchronized public void restart() throws Exception {
        // Don't do anything if the job is already running
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
        process = getJobRunner().start(locks);
        return process;
    }


    /*
      * (non-Javadoc)
      *
      * @see java.lang.Runnable#run()
      */
    synchronized final public void run(EntityManager em, Transaction transaction) throws Exception {
        // Those locks are transfered to the process
        ArrayList<Lock> locks = new ArrayList<>();
        // Those locks are used only in case of problem to unlock everything
        ArrayList<Lock> depLocks = new ArrayList<>();

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
                    locks.add(getMainConnector().createLockFile(LOCK_EXTENSION.transform(getPath()), false));
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
                LOGGER.debug("Running preparation - locking dependencies [%s]", this);
                for (Dependency dependency : getDependencies()) {
                    try {
                        LOGGER.debug("Running preparation - locking dependency [%s]", dependency);
                        dependency.from.lock(transaction, true); // Ensures the dependency does not change
                        em.refresh(dependency);
                        em.refresh(dependency.from);

                        final Lock lock = dependency.lock(em, pid);
                        depLocks.add(lock);
                        LOGGER.debug("Running preparation - locked dependency [%s]", dependency);
                    } catch (LockException e) {
                        // Update & store this dependency
                        Resource resource = dependency.getFrom();
                        e.addContext("While locking status run %s", resource);
                        throw e;
                    } catch (PersistenceException e) {
                        LOGGER.debug(e, "Persistence exception [%s] while locking dependency [%s]", e, dependency);
                        final LockException lockException = new LockException(e);
                        Resource resource = dependency.getFrom();
                        lockException.addContext("While locking status run %s", resource);
                        throw lockException;
                    }

                }

                // And run!
                LOGGER.info("Locks are OK. Running task [%s]", this);

                // Change the state
                setState(ResourceState.RUNNING);
                startTimestamp = System.currentTimeMillis();

                // Commits all the changes so far
                transaction.boundary(true);

                // Now, starts the job
                process = startJob(locks);
                process.adopt(locks);
                locks = null;
                transaction.boundary();

                // Store the current state
                LOGGER.info("Task [%s] is running (start=%d) with PID [%s]", this, startTimestamp, process.getPID());
                for (Dependency dep : getDependencies()) {
                    LOGGER.info("[STARTED JOB] Dependency: %s", dep);
                }

                // Flush to database

                break;
            }
        } catch (LockException e) {
            LOGGER.warn("Could not lock job %s or one of its dependencies", this);
            throw e;
        } catch (RollbackException e) {
            LOGGER.info("Could not commit - rolling back", this);
            throw e;
        } catch (RuntimeException e) {
            LOGGER.error(e, "Caught exception for %s", this);
            throw e;
        } catch (Throwable e) {
            LOGGER.error(e, "Caught exception for %s", this);
            throw new RuntimeException(e);
        } finally {
            // Dispose of the locks that we own
            if (locks != null) {
                if (process != null) {
                    LOGGER.info("An error occurred: disposing process");
                    process.destroy();
                }

                LOGGER.info("An error occurred: disposing locks");
                for (Lock lock : Iterables.concat(locks, depLocks)) {
                    try {
                        LOGGER.info("Disposing of lock %s", lock);
                        lock.close();
                    } catch (Throwable e) {
                        LOGGER.error(e, "Could not close lock %s", lock);
                    }
                }
            }
        }


    }


    /**
     * Called when a resource state has changed. After an update, the entity will be
     * saved to the database and further cascading operations make take place.
     *
     * @param t       The current transaction
     * @param em      The current entity manager
     * @param message The message
     */
    @Override
    public void notify(Transaction t, EntityManager em, Message message) {
        LOGGER.debug("Notification [%s] for job [%s]", message, this);

        switch (message.getType()) {
            case RESOURCE_REMOVED:
                clean();
                break;

            case END_OF_JOB:
                // First, register our changes
                endOfJobMessage((EndOfJobMessage) message, em, t);
                t.boundary();
                break;

            case DEPENDENCY_CHANGED:
                // Retrieve message
                final DependencyChangedMessage depMessage = (DependencyChangedMessage) message;

                // Notify job
                dependencyChanged(depMessage);
                t.boundary();

                LOGGER.debug("After notification [%s], state is %s [from %s] for [%s]",
                        depMessage.toString(), getState(), oldState, this);

                break;

            default:
                super.notify(t, em, message);
        }
    }

    /**
     * Called when a dependency has changes.
     * <p>
     * It performs the changes in the object but to not save it.
     *
     * @param message The message
     */
    private void dependencyChanged(DependencyChangedMessage message) {
        LOGGER.debug("[before] Locks for job %s: unsatisfied=%d, holding=%d", this, nbUnsatisfied, nbHolding);

        int diff = (message.newStatus.isOK() ? 1 : 0) - (message.oldStatus.isOK() ? 1 : 0);
        int diffHold = (message.newStatus.isBlocking() ? 1 : 0) - (message.oldStatus.isBlocking() ? 1 : 0);

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
            assert nbUnsatisfied >= nbHolding : String.format("Number of unsatisfied (%d) < number of holding (%d)",
                    nbUnsatisfied, nbHolding);
        }
        LOGGER.debug("[after] Locks for job %s: unsatisfied=%d, holding=%d [%d/%d] in %s -> %s", this, nbUnsatisfied, nbHolding,
                diff, diffHold, message.fromId, message.newStatus);
    }

    /**
     * Called when the job has ended
     *
     * @param eoj The message
     * @param em  The entity manager
     * @param t   The transaction
     */
    private void endOfJobMessage(EndOfJobMessage eoj, EntityManager em, Transaction t) {
        this.endTimestamp = eoj.timestamp;

        // Lock all the required dependencies and refresh

        LOGGER.info("Job %s has ended with code %d", this, eoj.code);

        // (1) Release required resources
        LOGGER.debug("Release dependencies of job [%s]", this);
        try {
            final Collection<Dependency> requiredResources = getDependencies();
            for (Dependency dependency : requiredResources) {
                try {
                    dependency.from.lock(t, true);
                    em.refresh(dependency.from);
                    em.refresh(dependency);
                    dependency.unlock(em);

                } catch (Throwable e) {
                    LOGGER.error(e, "Error while unlocking dependency %s", dependency);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error(e, "Error while unactivating dependencies");
        }


        // (2) dispose old XPM process

        try {
            if (process != null) {
                LOGGER.debug("Disposing of old XPM process [%s]", process);
                process.dispose();
                process = null;
            } else {
                LOGGER.warn("There was no XPM process attached...");
            }
        } catch (Exception e) {
            LOGGER.error("Could not dispose of the old process checker %s", e);
        }

        // (3) Change state (DONE or ERROR depending on the end of job status code)
        setState(eoj.code == 0 ? ResourceState.DONE : ResourceState.ERROR);
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

        Collection<Dependency> requiredResources = getDependencies();
        if (!requiredResources.isEmpty()) {
            JSONArray dependencies = new JSONArray();
            info.put("dependencies", dependencies);

            for (Dependency dependency : requiredResources) {
                Resource resource = dependency.getFrom();

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

                Resource resource = dependency.getFrom();

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

    /**
     * Add a dependency (requirement) for this job.
     *
     * @param dependency The dependency
     */
    public void addDependency(Dependency dependency) {
        if (prepared) {
            throw new AssertionError("Adding dependency on a saved resource");
        }
        // We do not add it to the source dependency since
        // this will be done latter
        // TODO: check if this is not done latter... remove ?
        addIngoingDependency(dependency);
        dependency.update();
        if (!dependency.status.isOK()) {
            nbUnsatisfied++;
        }

    }


    @Override
    synchronized protected boolean doUpdateStatus() throws Exception {
        LOGGER.debug("Updating status for [%s]", this);
        boolean changes = super.doUpdateStatus();

        // Check the done file
        final Path doneFile = DONE_EXTENSION.transform(getPath());
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

            for (Dependency dependency : getDependencies()) {
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

            LOGGER.debug("After update, state of %s is %s [unsatisfied=%d, holding=%d]", this, state, nbUnsatisfied, nbHolding);
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
    private void removeJobFile(FileNameTransformer t) {
        try {
            final Path file = t.transform(getPath());
            if (Files.exists(file)) {
                Files.delete(file);
            }
        } catch (IOException e) {
            LOGGER.info(e, "Could not remove '%s' file: %s / %s", getLocator(), t);
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

        // Dependencies of job runner have been taken care of
        // no need to add them
        this.jobRunnerString = job.jobRunnerString;
        this.jobRunner = job.getJobRunner();
    }


    public void setJobRunner(JobRunner jobRunner) {
        if (this.jobRunnerString != null) {
            throw new AssertionError("Job runner has already been set");
        }

        // Sets the job runner and its string version
        this.jobRunner = jobRunner;
        this.jobRunnerString = JobRunnerConverter.INSTANCE.convertToDatabaseColumn(jobRunner);

        this.getJobRunner().job = this;

        // Adds all dependencies from the job runner
        jobRunner.dependencies().forEach(this::addIngoingDependency);
    }

    @Override
    public void stored() {
        super.stored();

        if (getState() == ResourceState.READY) {
            LOGGER.debug("Job is READY, notifying");
            Scheduler.notifyRunners();
        }

    }

    @Override
    public Path outputFile() throws IOException {
        return getJobRunner().outputFile(this);
    }

    @PostLoad
    protected void postLoad() {
        super.postLoad();
    }

    public JobRunner getJobRunner() {
        if (jobRunnerString != null && jobRunner == null) {
            jobRunner = JobRunnerConverter.INSTANCE.convertToEntityAttribute(jobRunnerString);
            jobRunner.job = this;
        }
        return jobRunner;
    }

    public XPMProcess getProcess() {
        return process;
    }
}
