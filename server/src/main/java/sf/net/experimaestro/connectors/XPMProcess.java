package sf.net.experimaestro.connectors;

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

import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.scheduler.*;
import sf.net.experimaestro.utils.log.Logger;

import javax.persistence.*;
import java.io.*;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
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
 */
@Entity
@Table(name = "process")
@DiscriminatorColumn(name = "type")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class XPMProcess {
    static private Logger LOGGER = Logger.getLogger();

    /**
     * The checker
     */
    protected transient ScheduledFuture<?> checker = null;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    /**
     * The job to notify when finished with this
     */
    @OneToOne(fetch= FetchType.LAZY, mappedBy = "process")
    protected Job job;

    /**
     * Our process ID
     */
    String pid;

    /**
     * The host where this process is running (or give an access to the process, e.g. for OAR processes)
     */
    @ManyToOne
    SingleHostConnector connector;

    /**
     * The associated locks to release when the process has ended
     */
    @OneToMany(cascade = CascadeType.ALL)
    private List<Lock> locks = null;

    /**
     * Creates a new job monitor from a process
     *
     * @param job The attached job (If any)
     * @param pid The process ID
     */
    protected XPMProcess(SingleHostConnector connector, String pid, final Job job) {
        this.connector = connector;
        this.pid = pid;
        this.job = job;
    }

    /**
     * Constructs a XPMProcess without an underlying process
     */
    protected XPMProcess(SingleHostConnector connector, final Job job, String pid) {
        this(connector, pid, job);
    }

    /**
     * Used for serialization
     *
     * @see {@linkplain #init(sf.net.experimaestro.scheduler.Job)}
     */
    protected XPMProcess() {
    }

    /**
     * Set up a notifiction using {@linkplain java.lang.Process#waitFor()}.
     */

    protected void startWaitProcess() {
        LOGGER.debug("XPM Process %s constructed", connector);

        // Set up the notification thread if needed
        if (job != null) {
            new Thread(String.format("job monitor [%s]", job.getId())) {
                @Override
                public void run() {
                    int code;
                    while (true) {
                        try {
                            LOGGER.info("Waiting for job [%s] process to finish", job);
                            code = waitFor();
                            LOGGER.info("Job [%s] process has finished running [code %d]", job, code);
                            break;
                        } catch (InterruptedException e) {
                            LOGGER.error("Interrupted: %s", e);
                        }
                    }

                    try {
                        final int _code = code;
                        Transaction.run((em, t) -> job.notify(t, em, new EndOfJobMessage(_code, System.currentTimeMillis())));
                    } catch (RuntimeException e) {
                        LOGGER.warn(e, "Failed to notify end-of-job for %s", job);
                    }
                }
            }.start();
        }
    }

    /**
     * Initialization of the job monitor (when restoring from database)
     * <p/>
     * The method also sets up a status checker at regular intervals.
     */
    public void init(Job job)  {
        // TODO: use connector & job dependent times for checking
        checker = Scheduler.get().schedule(this, 15, TimeUnit.SECONDS);
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
            } catch (Throwable e) {
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
        return Files.exists(Job.LOCK_EXTENSION.transform(job.getPath()));
    }

    /**
     * Returns the error code
     */
    public int exitValue() {
        // Check for done file
        try {
            if (Files.exists(Resource.DONE_EXTENSION.transform(job.getPath())))
                return 0;

            // If the job is not done, check the ".code" file to get the error code
            // and otherwise return -1
            final Path codeFile = Resource.CODE_EXTENSION.transform(job.getPath());
            if (Files.exists(codeFile)) {
                final InputStream stream = Files.newInputStream(codeFile);
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
     * @param lock The lock to add
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
            final Path file = Resource.CODE_EXTENSION.transform(job.getPath());
            final long time = Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() : -1;
            Transaction.run((em, t) -> job.notify(t, em, new EndOfJobMessage(exitValue(), time)));
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

                } catch (InterruptedException ignored) {

                }
            }
        }
    }

    /**
     * @see {@linkplain Process#destroy()}
     */
    abstract public void destroy() throws FileSystemException;

    public String getPID() {
        return pid;
    }
}
