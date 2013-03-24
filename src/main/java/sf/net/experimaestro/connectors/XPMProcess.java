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

package sf.net.experimaestro.connectors;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.Persistent;
import org.apache.commons.vfs2.FileObject;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.scheduler.EndOfJobMessage;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

import java.io.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A process monitor.
 * <p/>
 * <p>
 * A job monitor task is to... monitor the execution of a job, wherever
 * the job is running (remote, local, etc.).
 * It is returned when a job starts, contains information (ID, state, etc.) and control methods (stop)
 * </p>
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date June 2012
 */
@Persistent
public abstract class XPMProcess {
    static private Logger LOGGER = Logger.getLogger();

    /**
     * Our process ID
     */
    String pid;

    /**
     * The host where this process is running (or give an access to the process, e.g. for OAR processes)
     */
    transient SingleHostConnector connector;
    String connectorId;

    /**
     * The associated locks to release when the process has ended
     */
    private List<Lock> locks = null;

    /**
     * The checker
     */
    protected transient ScheduledFuture<?> checker = null;

    /**
     * The job to notify when finished with this
     */
    protected transient Job job;

    /**
     * Creates a new job monitor from a process
     *
     * @param job    The attached job (If any)
     * @param pid    The process ID
     * @param notify If a notification should be put in place using {@linkplain java.lang.Process#waitFor()}.
     *               Otherwise, it is the caller job to set it up.
     */
    protected XPMProcess(SingleHostConnector connector, String pid, final Job job, boolean notify) {
        this.connector = connector;
        this.pid = pid;
        this.job = job;
        this.connectorId = connector.getIdentifier();

        LOGGER.debug("XPM Process %s constructed", connectorId);

        // Set up the notification thread if needed
        if (notify && job != null) {
            new Thread(String.format("job monitor [%s]", job.getId())) {
                @Override
                public void run() {
                    int code = 0;
                    while (true) {
                        try {
                            LOGGER.info("Waiting for task %s to finish", job.getIdentifier());
                            code = waitFor();
                            LOGGER.info("Task %s has finished running", job.getIdentifier());
                            break;
                        } catch (InterruptedException e) {
                            LOGGER.error("Interrupted: %s", e);
                        }
                    }

                    job.notify(null, new EndOfJobMessage(code, System.currentTimeMillis()));
                }
            }.start();
        }
    }

    /**
     * Constructs a XPMProcess without an underlying process
     */
    protected XPMProcess(SingleHostConnector connector, final Job job, String pid) {
        this(connector, pid, job, false);
    }


    /**
     * Used for serialization
     *
     * @see {@linkplain #init(sf.net.experimaestro.scheduler.Job)}
     */
    protected XPMProcess() {
    }

    /**
     * Initialization of the job monitor (when restoring from database)
     * <p/>
     * The method also sets up a status checker at regular intervals.
     */
    public void init(Job job) throws DatabaseException {
        this.job = job;
        final Scheduler scheduler = job.getScheduler();

        // TODO: use connector & job dependent times for checking
        checker = scheduler.schedule(this, 15, TimeUnit.SECONDS);

        connector = (SingleHostConnector) scheduler.getConnector(connectorId);

        // Init locks if needed
        for (Lock lock : locks)
            lock.init(scheduler);
    }

    @Override
    public String toString() {
        return String.format("[Process of %s]", job);
    }

    /**
     * Get the underlying job
     *
     * @return The job
     */
    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }


    /**
     * Adopt the locks
     */
    public void adopt(List<Lock> locks) {
        // Changing the ownership of the different logs
        this.locks = locks;
        for (Lock lock : locks) {
            try {
                lock.changeOwnership(pid);
            } catch (LockException e) {
                LOGGER.error("Could not adopt lock %s", lock);
            }
        }
    }

    /**
     * Dispose of this job monitor
     */
    public void dispose() {
        close();
        if (locks != null) {
            LOGGER.info("Disposing of %d locks for %s", locks.size(), this);
            while (!locks.isEmpty()) {
                locks.get(locks.size() - 1).close();
                locks.remove(locks.size() - 1);
            }

            locks = null;
        }

    }

    /**
     * Close this job monitor
     */
    void close() {
        if (checker != null) {
            LOGGER.info("Cancelling the checker for %s", this);
            if (!checker.cancel(true)) {
                checker = null;
            } else
                LOGGER.warn("Could not cancel the checker");

        }
    }


    /**
     * Check if the job is running
     *
     * @return True if the job is running
     * @throws Exception
     */
    public boolean isRunning() throws Exception {
        // We have no process, check
        return job.getMainConnector().resolveFile(job.getLocator().getPath() + Job.LOCK_EXTENSION).exists();

    }

    /**
     * Returns the error code
     */
    public int exitValue() {
        // Check for done file
        try {
            if (job.getLocator().resolve(connector, Resource.DONE_EXTENSION).exists())
                return 0;

            // If the job is not done, check the ".code" file to get the error code
            // and otherwise return -1
            final FileObject codeFile = job.getMainConnector().resolveFile(job.getLocator().getPath() + Job.CODE_EXTENSION);
            if (codeFile.exists()) {
                final InputStream stream = codeFile.getContent().getInputStream();
                final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String s = reader.readLine();
                int code = s != null ? Integer.parseInt(s) : -1;
                reader.close();
                return code;
            }
        } catch (IOException e) {
            throw new IllegalThreadStateException();
        }

        // The process failed, but we do not know how
        return -1;

    }

    /**
     * Add a lock to release after this job has completed
     *
     * @param lock
     */
    public void addLock(Lock lock) {
        locks.add(lock);
    }

    /**
     * Asynchronous check the state of the job monitor
     */
    public void check() throws Exception {
        if (!isRunning()) {
            // We are not running: send a message
            LOGGER.debug("End of job [%s]", job);
            final FileObject file = job.getLocator().resolve(connector, Resource.CODE_EXTENSION);
            final long time = file.exists() ? file.getContent().getLastModifiedTime() : -1;
            job.notify(null, new EndOfJobMessage(exitValue(), time));
            dispose();
        }
    }

    /**
     * @see {@linkplain Process#getOutputStream()}
     */
    abstract public OutputStream getOutputStream();

    /**
     * @see {@linkplain Process#getErrorStream()}
     */
    abstract public InputStream getErrorStream();

    /**
     * @see {@linkplain Process#getInputStream()}
     */
    abstract public InputStream getInputStream();

    /**
     * Periodic checks
     *
     * @see {@linkplain Process#waitFor()}
     */
    public int waitFor() throws InterruptedException {
        synchronized (this) {
            // Wait 1 second
            while (true) {
                try {
                    try {
                        if (!isRunning()) {
                            return exitValue();
                        }
                    } catch (Exception e) {
                        // TODO: what to do here?
                        // Delay?
                        LOGGER.warn("Error while checking if running");
                    }
                    wait(1000);

                } catch (InterruptedException e) {

                }
            }
        }
    }

    /**
     * @see {@linkplain Process#destroy()}
     */
    abstract public void destroy();

    public String getPID() {
        return pid;
    }
}
