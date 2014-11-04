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

import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.connectors.XPMProcess;
import sf.net.experimaestro.utils.ThreadCount;
import sf.net.experimaestro.utils.log.Logger;

import javax.persistence.Entity;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The process corresponding to a waiting job
 */
@Entity
class WaitingJobProcess extends XPMProcess {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * A scheduler for restarts
     */
    private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * The action
     */
    WaitingJobRunner.Action action;
    private long timestamp;
    private transient ThreadCount counter;

    public WaitingJobProcess() {
        LOGGER.debug("Default constructor for MyXPMProcess");
    }

    public WaitingJobProcess(ThreadCount counter, SingleHostConnector connector, Job job, WaitingJobRunner.Action action) {
        super(connector, "1", job);
        assert action != null;
        this.timestamp = System.currentTimeMillis();
        this.counter = counter;
        this.action = action;
        LOGGER.debug("XPM Process initialized for job " + job + " with action " + action);
        startWaitProcess();
    }

    @Override
    public void init(Job job) {
        LOGGER.debug("Initialized with job %s", job);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public int waitFor() throws InterruptedException {
        assert job.getStartTimestamp() > 0;
        synchronized (this) {
            LOGGER.debug("Starting to wait - " + job + " - " + action);
            long toWait = action.duration - (System.currentTimeMillis() - timestamp);
            if (toWait > 0) wait(toWait);
            LOGGER.debug("Ending the wait - %s (time = %d)", job, System.currentTimeMillis() - timestamp);
        }

        // Schedule a restart

        if (action.restart > 0) {
            scheduler.schedule(() -> {
                LOGGER.debug("Restarting job %s[%d]", job.getId());
                try {
                    ((WaitingJob)job).status().currentIndex++;
                    job.restart();
                } catch (Exception e) {
                    throw new AssertionError("Could not restart job", e);
                }
            }, action.restart, TimeUnit.MILLISECONDS);
        }

        counter.del();
        return action.code;
    }

    @Override
    public boolean isRunning() throws Exception {
        return action.duration < System.currentTimeMillis() - timestamp;
    }

    @Override
    public int exitValue() {
        return 0;
    }

    @Override
    public OutputStream getOutputStream() {
        return null;
    }

    @Override
    public InputStream getErrorStream() {
        return null;
    }

    @Override
    public InputStream getInputStream() {
        return null;
    }

    @Override
    public void destroy() {
    }


}
