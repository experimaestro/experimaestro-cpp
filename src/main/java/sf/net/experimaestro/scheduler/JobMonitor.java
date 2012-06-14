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

package sf.net.experimaestro.scheduler;

import com.sleepycat.persist.model.Persistent;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.ProcessUtils;
import sf.net.experimaestro.utils.log.Logger;

import java.util.List;

/**
 * Monitor the execution of a job.
 *
 * A job monitor task is to monitor the execution of a job.
 * It is returned when a job starts and contains informations about the running job.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 14/6/12
 */
@Persistent
public abstract class JobMonitor {
    static private Logger LOGGER = Logger.getLogger();

    /** Our process ID */
    String pid;

    /** Our job */
    transient Job job;


    /** Our locks */
    private List<Lock> locks;


    protected JobMonitor(String pid, Job job) {
        this.pid = pid;
        this.job = job;
    }

    protected JobMonitor() {
    }

    protected void finalize() {
        dispose();
    }

    void adopt(List<Lock> locks) {
        // Changing the ownership of the different logs
        for (Lock lock : locks) {
            lock.changeOwnership(pid);
        }
    }

    private void dispose() {
        if (locks != null) {
            LOGGER.info("Disposing of locks for %s", this);
            for (Lock lock : locks)
                lock.dispose();
        }
    }

    /**
     * Wait for it to finish
     *
     * By default, it waits a given amount of time and check the status
     */
    public int waitFor() throws Exception {
        while (isRunning()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted wait: %s", e);
            }
        }
        return getCode();
    }


    /** Set job */
    void setJob(Job job) {
        this.job = job;
    }

    /** Check status */
    abstract boolean isRunning() throws Exception;

    /** Error code */
    abstract int getCode() throws Exception;
}
