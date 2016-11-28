package net.bpiwowar.xpm.scheduler;

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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import net.bpiwowar.xpm.connectors.XPMProcess;
import net.bpiwowar.xpm.exceptions.LockException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.locks.FileLock;
import net.bpiwowar.xpm.locks.Lock;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.utils.FileNameTransformer;
import net.bpiwowar.xpm.utils.GsonSerialization;
import net.bpiwowar.xpm.utils.Holder;
import net.bpiwowar.xpm.utils.ProcessUtils;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Stream;

/**
 * A job is a resource that can be run - that starts and ends (which
 * differentiate it with a server) and generate data
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
abstract public class Job extends Resource {

    final static DateFormat longDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    final static private Logger LOGGER = Logger.getLogger();
    public static final ImmutableList<String> KILLED_ERROR_LINE = ImmutableList.of("1");

    transient private JobData jobData;

    /**
     * The process.
     * The holder is used to distinguish between the fact that the process was not
     * loaded (null holder) or that there is no process (null held value).
     */
    @GsonSerialization(serialize = false)
    private Holder<XPMProcess> process;

//    /**
//     * Requirements (ignored for the moment)
//     */
//    transient ComputationalRequirements requirements;


    /**
     * Initialisation of a task
     * <p>
     * The job is by default initialized as "WAITING": its state should be updated after
     * the initialization has finished
     */
    public Job(Path path) {
        super(path);
        jobData = new JobData(this);
    }

    /**
     * Initialize from database
     *
     * @param id
     * @param locator
     * @throws SQLException
     */
    public Job(long id, Path locator) throws SQLException {
        super(id, locator);
    }

    private boolean isDone() {
        try {
            return Files.exists(DONE_EXTENSION.transform(getLocator()));
        } catch (Exception e) {
            LOGGER.error("Error while checking if " + getLocator() + DONE_EXTENSION + " exists");
            return false;
        }
    }

    @Override
    public synchronized void delete(boolean recursive) throws SQLException {
        if (this.getState() != ResourceState.RUNNING) {
            final XPMProcess process = getProcess();
            if (process != null) {
                LOGGER.error("Removing stale process");
                try {
                    process.dispose();
                } catch (LockException e) {
                    throw new SQLException(e);
                }
            }
        }
        super.delete(recursive);
    }

    /**
     * Restart the job
     * <p>
     * Put the state into waiting mode and clean all the output files
     *
     * @param restart True if jobs should be restarted, otherwise put it in error
     */
    synchronized public void invalidate(boolean restart) throws Exception {
        // Don't do anything if the job is already running
        if (!getState().isActive()) {
            // Set state status waiting
            setState(restart ? ResourceState.WAITING : ResourceState.ERROR);

            if (getState().isFinished()) {
                clean(false);

                if (!restart) {
                    // Put back the error file
                    try (final BufferedWriter writer = Files.newBufferedWriter(CODE_EXTENSION.transform(getLocator()))) {
                        writer.write(1);
                        writer.write('\n');
                    }
                }

            } else {
                // Remove blocking files
                try (FileLock lock = FileLock.of(LOCK_EXTENSION.transform(getLocator()), 5)) {
                    Files.deleteIfExists(DONE_EXTENSION.transform(getLocator()));
                    Files.deleteIfExists(CODE_EXTENSION.transform(getLocator()));
                }
            }

            // Update status
            updateStatus();
            Scheduler.notifyRunners();
        }
    }

    @Override
    protected void finalize() {
    }

    /**
     * @return the priority
     */
    final public int getPriority() {
        return jobData().getPriority();
    }


    /**
     * TaskReference priority - the higher, the better
     *
     * @param priority the priority status set
     */
    final public void setPriority(int priority) {
        jobData().setPriority(priority);
    }

    public long getTimestamp() {
        return jobData().getTimestamp();
    }

    /**
     * This is where the real job gets done
     *
     * @param locks The locks that were taken
     * @param fake  Do everything as if starting but do not start the process
     * @return The process corresponding status the job
     * @throws Throwable If something goes wrong <b>before</b> starting the process. Otherwise, it should
     *                   return the process
     */
    protected XPMProcess startJob(ArrayList<Lock> locks, boolean fake) throws Throwable {
        if (fake) {
            start(locks, true);
        } else {
            setProcess(start(locks, fake));
        }
        return getProcess();
    }

    private void setProcess(XPMProcess process) throws SQLException {
        if (getProcess() != null && process != null) {
            throw new AssertionError("Should not set a process when we already have one");
        }

        if (process != null) {
            process.save();
        } else {
            try {
                this.process.get().dispose();
            } catch (LockException e) {
                throw new SQLException(e);
            }
        }
        this.process.set(process);
    }

    public void generateFiles() throws Throwable {
        startJob(Lists.newArrayList(), true);
    }

    /**
     * Checks that no process is currently running, and update the job state accordingly.
     * <p>
     * This is mostly used when the database was corrupted.
     *
     * @return true if a no process is running, false
     * @throws Exception If something goes wrong while checking
     */
    public boolean checkProcess() throws Exception {
        final XPMProcess process = getProcess();
        if (process != null) {
            if (process.isRunning()) {
                setState(ResourceState.RUNNING);
            } else {
                Scheduler.get().sendMessage(this, new EndOfJobMessage(process.exitValue(false), process.exitTime()));
            }

            return false;
        }

        return true;
    }

    /*
      * (non-Javadoc)
      *
      * @see java.lang.Runnable#run()
      */
    synchronized final public void run() throws Exception {
        // Those locks are transfered to the process
        ArrayList<Lock> locks = new ArrayList<>();

        jobData();

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
                    // Create the directory if it does not exist
                    Files.createDirectories(getLocator().getParent());
                    locks.add(FileLock.of(LOCK_EXTENSION.transform(getLocator()), false));
                } catch (LockException e) {
                    LOGGER.info(e, "Could not lock job [%s]: %s", this, e);
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
                        dependency.lock(pid);
                        LOGGER.debug("Running preparation - locked dependency [%s]", dependency);
                    } catch (LockException e) {
                        // Update & store this dependency
                        Resource resource = dependency.getFrom();
                        e.addContext("While locking status run %s", resource);
                        throw e;
                    }
                }

                // And run!
                LOGGER.info("Locks are OK. Running task [%s]", this);

                // Change the state
                setState(ResourceState.RUNNING);
                jobData.setStartTimestamp(System.currentTimeMillis());

                // Commits all the changes so far

                // Now, starts the job
                startJob(locks, false);
                getProcess().adopt(locks);
                locks = null;

                // Store the current state
                LOGGER.info("Task [%s] is running (start=%d) with PID [%s]", this, jobData.getStartTimestamp(), getProcess());
                for (Dependency dep : getDependencies()) {
                    LOGGER.debug("[STARTED JOB] Dependency: %s", dep);
                }

                // Flush to database

                break;
            }
        } catch (LockException e) {
            LOGGER.warn("Could not lock job %s or one of its dependencies", this);
            throw e;
        } catch (RuntimeException e) {
            LOGGER.error(e, "Caught exception for %s", this);
            throw e;
        } catch (Throwable e) {
            LOGGER.error(e, "Caught exception for %s", this);
            throw new RuntimeException(e);
        } finally {
            if (locks != null) {
                LOGGER.info("An error occurred: killing process and unlocking job %s", this);

                // Dispose of process
                if (getProcess() != null) {
                    LOGGER.info("An error occurred: disposing process");
                    getProcess().destroy();
                }

                // Dispose of dependency locks
                for (Dependency dependency : getDependencies()) {
                    try {
                        if (dependency.hasLock()) {
                            dependency.unlock();
                        }
                    } catch (Throwable e) {
                        LOGGER.error(e, "Could not close lock of dependency %s", dependency);
                    }
                }

                // Dispose of locks
                if (locks != null) {
                    for (Lock lock : Iterables.concat(locks)) {
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


    }


    /**
     * Called when a resource state has changed. After an update, the entity will be
     * saved to the database and further cascading operations make take place.
     *
     * @param message The message
     */
    @Override
    public void notify(Message message) throws SQLException {
        LOGGER.debug("Notification [%s] for job [%s]", message, this);

        switch (message.getEvent()) {
            case RESOURCE_REMOVED:
                break;

            case END_OF_JOB:
                // First, register our changes
                endOfJobMessage((EndOfJobMessage) message);
                Scheduler.get().addChangedResource(this);
                break;

            case DEPENDENCY_CHANGED:
                // Retrieve message
                final DependencyChangedMessage depMessage = (DependencyChangedMessage) message;

                // Notify job
                ResourceState oldState = getState();
                dependencyChanged(depMessage);

                LOGGER.debug("After notification [%s], state is %s [from %s] for [%s]",
                        depMessage.toString(), getState(), oldState, this);

                break;
        }

        super.notify(message);
    }

    /**
     * Called when a dependency has changes.
     * <p>
     * It performs the changes in the object but to not save it.
     *
     * @param message The message
     */
    synchronized private void dependencyChanged(DependencyChangedMessage message) throws SQLException {
        assert message.dependency.getTo() == this;

        jobData();

        int diff = (message.newStatus.isOK() ? 1 : 0) - (message.oldStatus.isOK() ? 1 : 0);
        int diffHold = (message.newStatus.isBlocking() ? 1 : 0) - (message.oldStatus.isBlocking() ? 1 : 0);
        LOGGER.debug("[before] Locks for job %s: unsatisfied=%d, holding=%d [%d/%d]",
                this, jobData.getNbUnsatisfied(), jobData.getNbHolding(), diff, diffHold);

        if (diff != 0 || diffHold != 0) {
            jobData.setRequired(jobData.getNbUnsatisfied() - diff, jobData.getNbHolding() + diffHold);
            // Store the result
            assert jobData.getNbHolding() >= 0;
            assert jobData.getNbUnsatisfied() >= jobData.getNbHolding() : String.format("[job %s] Number of unsatisfied (%d) < number of holding (%d)",
                    this, jobData.getNbUnsatisfied(), jobData.getNbHolding());

            // Change the state in function of the number of unsatisfied requirements
            if (jobData.getNbUnsatisfied() == 0) {
                setState(ResourceState.READY);
            } else {
                if (jobData.getNbHolding() > 0)
                    setState(ResourceState.ON_HOLD);
                else
                    setState(ResourceState.WAITING);
            }

            // Save dependency
            message.dependency.save(true);
        }
        LOGGER.debug("[after] Locks for job %s: unsatisfied=%d, holding=%d [%d/%d] in %s -> %s", this, jobData.getNbUnsatisfied(), jobData.getNbHolding(),
                diff, diffHold, message, message.newStatus);
    }

    /**
     * Called when the job has ended
     *
     * @param eoj The message
     */
    synchronized private void endOfJobMessage(EndOfJobMessage eoj) throws SQLException {
        jobData();
        jobData.setEndTimestamp(eoj.timestamp);

        // Lock all the required dependencies and refresh

        LOGGER.info("Job %s has ended with code %d", this, eoj.code);

        // (1) Release required resources
        LOGGER.debug("Release dependencies of job [%s]", this);
        try {
            final Collection<Dependency> requiredResources = getDependencies();
            for (Dependency dependency : requiredResources) {
                try {
                    dependency.unlock();
                } catch (Throwable e) {
                    LOGGER.error(e, "Error while unlocking dependency %s", dependency);
                }
            }

        } catch (RuntimeException e) {
            LOGGER.error(e, "Error while unactivating dependencies");
        }


        // (2) dispose old XPM process

        try {
            if (getProcess() != null) {
                LOGGER.debug("Disposing of old XPM process [%s]", getProcess());
                setProcess(null);
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
        return jobData().getStartTimestamp();
    }

    public long getEndTimestamp() {
        return jobData().getEndTimestamp();
    }

    @Override
    public JsonObject toJSON() throws IOException {
        jobData();

        JsonObject info = super.toJSON();

        if (getState() == ResourceState.DONE
                || getState() == ResourceState.ERROR
                || getState() == ResourceState.RUNNING) {
            long start = getStartTimestamp();
            long end = getState() == ResourceState.RUNNING ? System
                    .currentTimeMillis() : getEndTimestamp();

            JsonObject events = new JsonObject();
            info.add("process", events);
            info.addProperty("progress", getProgress());

            events.addProperty("start", longDateFormat.format(new Date(start)));

            if (getState() != ResourceState.RUNNING && end >= 0) {
                events.addProperty("end", longDateFormat.format(new Date(end)));
            }
            if (getProcess() != null)
                events.addProperty("pid", getProcess().getPID());
        }

        Collection<Dependency> requiredResources = getDependencies();
        if (!requiredResources.isEmpty()) {
            JsonObject dependencies = new JsonObject();
            info.add("dependencies", dependencies);

            for (Dependency dependency : requiredResources) {
                Resource resource = dependency.getFrom();

                JsonObject dep = new JsonObject();
                dependencies.add(resource.getId().toString(), dep);
                dep.addProperty("from-id", resource.getId());
                dep.addProperty("from", resource.getLocator().toString());
                dep.addProperty("status", dependency.status.toString());
                dep.addProperty("locked", dependency.hasLock());
            }
        }

        return info;
    }

    /**
     * Add a dependency (requirement) for this job.
     *
     * @param dependency The dependency
     */
    synchronized public void addDependency(Dependency dependency) {
        dependency.update();
        switch (dependency.accept()) {
            case OK_LOCK:
            case OK:
            case UNACTIVE:
                break;

            case ERROR:
            case HOLD:
                jobData();
                jobData.setRequired(jobData.getNbUnsatisfied(), jobData.getNbHolding() + 1);
                break;

            case WAIT:
                jobData();
                jobData.setRequired(jobData.getNbUnsatisfied() + 1, jobData.getNbHolding());
                break;
        }

        addIngoingDependency(dependency);
    }


    @Override
    synchronized protected boolean doUpdateStatus() throws Exception {
        LOGGER.debug("Updating status for [%s]", this);
        boolean changes = super.doUpdateStatus();

        // Check process
        if (getProcess() != null) {
            if (!getProcess().check(true, 0)) {
                return true;
            }
        }

        // Check the done file
        final Path path = getLocator();
        final Path codeFile = CODE_EXTENSION.transform(path);
        final Path lockFile = LOCK_EXTENSION.transform(path);
        final XPMProcess process = getProcess();

        if (Files.exists(codeFile)) {
            final ResourceState newState;
            try (final BufferedReader bufferedReader = Files.newBufferedReader(codeFile)) {
                int code = 1;
                final String line = bufferedReader.readLine();
                try {
                    code = Integer.parseInt(line);
                } catch (NumberFormatException e) {
                    LOGGER.info("Could not isStopped exit code file (number format exception for [%s]) %s", line, this);
                }
                newState = code == 0 ? ResourceState.DONE : ResourceState.ERROR;
            }
            if (setState(newState)) {
                changes = true;
                if (this instanceof Job) {
                    jobData().setEndTimestamp(Files.getLastModifiedTime(codeFile).toMillis());
                }
            }
        } else if (Files.exists(lockFile)) {
            ResourceState newState = ResourceState.RUNNING;
            if (setState(newState)) {
                changes = true;
            }
        } else {
            if (getState().isFinished()) {
                changes = true;
                this.setState(ResourceState.WAITING);
            } else if (getState() == ResourceState.RUNNING) {
                if (process != null) {
                    if (!process.isRunning()) {
                        Scheduler.get().sendMessage(this, new EndOfJobMessage(process.exitValue(false), process.exitTime()));
                    }
                } else {
                    // Null process, no lockfile: reset
                    setState(ResourceState.ON_HOLD);
                }
            }
        }

        // If DONE or ERROR, isStopped that process is removed
        if (process != null && (getState() == ResourceState.DONE || getState() == ResourceState.ERROR)) {
            if (process.isRunning()) {
                LOGGER.error("Incoherency: process is running but resource is DONE/ERROR");
                setState(ResourceState.RUNNING);
                return true;
            }

            LOGGER.warn("Removing dandling process for %s", this);
            setProcess(null);
        }


        // Check dependencies if we are in waiting or ready
        if (getState() == ResourceState.WAITING || getState() == ResourceState.READY || getState() == ResourceState.ON_HOLD) {
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

            changes = jobData().setRequired(nbUnsatisfied, nbHolding);

            LOGGER.debug("After update, state of %s is %s [unsatisfied=%d, holding=%d]", this, state, nbUnsatisfied, nbHolding);
            changes |= setState(state);
        }

        if (changes && inDatabase()) {
            jobData.save(true, getId());
        }
        return changes;
    }

    /**
     * Stop the job
     */
    public boolean stop() throws SQLException {
        // Process is running
        if (getProcess() != null) {
            try {
                getProcess().destroy();
                try {
                    Files.write(getFileWithExtension(CODE_EXTENSION), KILLED_ERROR_LINE);
                } catch (IOException e) {
                    LOGGER.error("Could not create error file");
                }
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

    @Override
    public void clean(boolean removeFile) {
        super.clean(removeFile);
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
            final Path file = t.transform(getLocator());
            if (Files.exists(file)) {
                Files.delete(file);
            }
        } catch (IOException e) {
            LOGGER.info(e, "Could not remove '%s' file: %s / %s", getLocator(), t);
        }
    }


    @Override
    public ReadWriteDependency createDependency(DependencyParameters object) {
        // TODO: assert object is nothing
        return new ReadWriteDependency(this);
    }

    @Override
    public void save() throws SQLException {
        // Save
        super.save();
    }

    @Override
    synchronized protected void save(DatabaseObjects<Resource, Void> resources, Resource old) throws SQLException {
        // Update status
        boolean update = this.inDatabase() || old != null;

        // Save resource
        super.save(resources, old);

        // Execute
        jobData().save(update, getId());

        LOGGER.debug("Resource %s saved/updated [%s]", this, jobData);
    }

    public XPMProcess getProcess() {
        if (getId() == null) {
            return null;
        }

        if (process == null) {
            try {
                process = new Holder(XPMProcess.load(this));
            } catch (SQLException e) {
                throw new XPMRuntimeException(e, "Error while loading process from database");
            }
        }
        return process.get();
    }

    private JobData jobData() {
        if (jobData == null) {
            jobData = new JobData(this);
        }
        return jobData;
    }

    public boolean isActiveWaiting() {
        return true;
    }


    public double getProgress() {
        final XPMProcess process = getProcess();
        if (process == null) {
            return -1;
        }
        return process.getProgress();
    }

    public void setProgress(double progress) {
        final XPMProcess process = getProcess();
        if (process != null) {
            process.setProgress(progress);
        } else {
            LOGGER.warn("Cannot set the process for %s", this);
        }
    }

    /**
     * This is where the real job gets done
     *
     * @param locks The locks that were taken
     * @param fake  Use this to prepare everything without starting the process
     * @return The process corresponding status the job (or null if fake is true)
     * @throws Throwable If something goes wrong <b>before</b> starting the process. Otherwise, it should
     *                   return the process (unless fake is true)
     */
    public abstract XPMProcess start(ArrayList<Lock> locks, boolean fake) throws Exception;

    /**
     * Start a process
     *
     * @param locks The locks that were taken
     * @return The process corresponding status the job
     * @throws Throwable If something goes wrong <b>before</b> starting the process. Otherwise, it should
     *                   return the process
     */
    public XPMProcess start(ArrayList<Lock> locks) throws Exception {
        return start(locks, false);
    }

    public abstract Stream<? extends Dependency> dependencies();

    public int getNbUnsatisfied() {
        return jobData().getNbUnsatisfied();
    }
}
