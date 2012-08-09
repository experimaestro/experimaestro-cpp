/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package sf.net.experimaestro.connectors;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.Persistent;
import org.apache.commons.vfs2.FileObject;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.scheduler.*;
import sf.net.experimaestro.scheduler.Process;
import sf.net.experimaestro.utils.log.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A process monitor.
 *
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
public class XPMProcess {
    static private Logger LOGGER = Logger.getLogger();

    /**
     * Our process ID
     */
    String pid;

    /**
     * Our locks
     */
    private List<Lock> locks = null;

    /**
     * The checker
     */
    protected transient ScheduledFuture<?> checker = null;

    /**
     * Our job
     */
    protected transient Job job;

    /**
     * Our underlying process if we have any
     */
    protected transient sf.net.experimaestro.scheduler.Process process;

    /**
     * Creates a new job monitor from a process
     *
     * @param job     The attached job
     * @param process The underlying process
     * @param notify  If a notification should be put in place using {@linkplain java.lang.Process#waitFor()}.
     *                Otherwise, it is the caller job to set it up.
     */
    protected XPMProcess(final Job job, final Process process, boolean notify) {
        this.pid = process.getPID();
        this.process = process;
        this.job = job;

        // Set up the notification thread if needed
        if (notify) {
            new Thread(String.format("job monitor [%s]", job.getLocator())) {
                @Override
                public void run() {
                    int code = 0;

                    while (true) {
                        try {
                            code = process.waitFor();
                            break;
                        } catch (InterruptedException e) {
                            LOGGER.error("Interrupted: %s", e);
                        }
                    }

                    job.notify(null, new EndOfJobMessage(code));
                }
            }.start();
        }
    }

    /** Constructs a XPMProcess without an underlying process */
    protected XPMProcess(final Job job, String pid) {
        this.job = job;
        this.pid = pid;
    }


    protected XPMProcess() {
    }

    /**
     * Adopt the locks
     */
    public void adopt(List<Lock> locks) {
        // Changing the ownership of the different logs
        this.locks = locks;
        for (Lock lock : locks) {
            lock.changeOwnership(pid);
        }
    }

    /**
     * Dispose of this job monitor
     */
    public void dispose() {
        close();
        if (locks != null) {
            LOGGER.info("Disposing of locks for %s", this);
            for (Lock lock : locks)
                lock.dispose();
        }

    }

    /**
     * Close this job monitor
     */
    void close() {
        if (checker != null) {
            checker.cancel(true);
            checker = null;
        }
    }


    protected void finalize() {
    }

    /**
     * Initialization of the job monitor (when restoring from database)
     * <p/>
     * The method also sets up a status checker at regular intervals.
     */
    public void init(Job job) throws DatabaseException {
        this.job = job;
        // TODO: use connector & job dependent times for checking
        checker = job.getScheduler().schedule(this, 15, TimeUnit.SECONDS);

        // Init locks if needed
        for(Lock lock: locks)
            lock.init(job.getScheduler());
    }

    /**
     * Check if the job is running
     *
     * @return True if the job is running
     * @throws Exception
     */
    public boolean isRunning() throws Exception {

        if (process != null)
            process.isRunning();

        // We have no process, check
        return job.getConnector().resolveFile(job.getLocator().getPath()+ Job.LOCK_EXTENSION).exists();

    }

    /**
     * Returns the error code
     */
    int getCode() throws Exception {
        // Use the process value
        if (process != null)
            return process.exitValue();

        // Check for done file
        if (job.getConnector().resolveFile(job.getLocator().getPath() + Job.DONE_EXTENSION).exists())
            return 0;

        // If the job is not done, check the ".code" file to get the error code
        // and otherwise return -1
        final FileObject codeFile = job.getConnector().resolveFile(job.getLocator().getPath() + Job.CODE_EXTENSION);
        if (codeFile.exists()) {
            final InputStream stream = codeFile.getContent().getInputStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String s = reader.readLine();
            int code = s != null ? Integer.parseInt(s) : -1;
            reader.close();
            return code;
        }

        // The process failed, but we do not know how
        return -1;

    }

    /**
     * Add a lock to release after this job has completed
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
            job.notify(null, new EndOfJobMessage(getCode()));
        }
    }

}
