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

import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.connectors.XPMProcess;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.ThreadCount;
import sf.net.experimaestro.utils.log.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

/**
 * A job that just waits a bit
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Entity
@DiscriminatorValue("-1")
public class WaitingJob extends JobRunner {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * A scheduler for restarts
     */
    private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * The statuses of the different jobs
     */
    static ArrayList<Status> statuses = new ArrayList<>();

    /**
     * The list of actions
     */
    ArrayList<Action> actions;

    /* The index in the static status list */
    int statusIndex;

    /* id for debugging */
    private String debugId;


    /**
     * Initialisation of a task
     * <p>
     * The job is by default initialized as "WAITING": its state should be updated after
     * the initialization has finished
     *
     * @param connector
     * @param path
     */
    public WaitingJob(Connector connector, Path path) {
        job = new Job(connector, path);
    }
    // The code to return

    public WaitingJob() {
    }

    public WaitingJob(ThreadCount counter, File dir, String debugId, Action... actions) {
        job = new Job(LocalhostConnector.getInstance(), new File(dir, debugId).toPath());

        Status status = new Status();
        synchronized (statuses) {
            statusIndex = statuses.size();
            statuses.add(status);
        }
        job.setJobRunner(this);
        status.counter = counter;
        this.debugId = debugId;
        counter.add(actions.length);

        this.actions = new ArrayList<>(Arrays.asList(actions));
        status.currentIndex = 0;

        // put ourselves in waiting mode (rather than ON HOLD default)
        job.setState(ResourceState.WAITING);
    }

    public Status status() {
        return statuses.get(statusIndex);
    }

    @Override
    public XPMProcess startJob(ArrayList<Lock> locks) {
        Status status = statuses.get(statusIndex);
        assert status.readyTimestamp > 0;
        if (status.currentIndex >= actions.size()) {
            throw new AssertionError("No next action");
        }

        final Action action = actions.get(status.currentIndex);
        return new MyXPMProcess(status.counter, job.getMainConnector(), job, action);
    }

    @Override
    Path outputFile(Job job) throws FileSystemException {
        return null;
    }

    @Override
    public Stream<Dependency> dependencies() {
        return Stream.of();
    }

    public int finalCode() {
        return actions.get(actions.size() - 1).code;
    }

    public void restart(Action action) {
        throw new NotImplementedException();
    }

    public static class Status {
        // Current index (action to run)
        int currentIndex;

        // When was the job ready to run
        long readyTimestamp = 0;

        // Counter
        transient private ThreadCount counter;
    }

    /**
     * An action of our job
     */
    static public class Action implements Serializable {
        // Duration before exiting
        long duration;

        // Exit code
        int code;

        // Waiting time before restart (0 = no restart)
        long restart;

        Action() {
        }

        Action(long duration, int code, long restart) {
            this.duration = duration;
            this.code = code;
            this.restart = restart;
        }

        @Override
        public String toString() {
            return String.format("Action(duration=%dms, code=%d, restart=%dms)", duration, code, restart);
        }
    }

    static private class MyXPMProcess extends XPMProcess {
        /**
         * The action
         */
        Action action;
        private long timestamp;
        private transient ThreadCount counter;

        public MyXPMProcess() {
            LOGGER.debug("Default constructor for MyXPMProcess");
        }

        public MyXPMProcess(ThreadCount counter, SingleHostConnector connector, Job job, Action action) {
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
                // "We should change the job index"
                throw new NotImplementedException();
//                scheduler.schedule(() -> {
//                    LOGGER.debug("Restarting job %s[%d]", job.getId());
//                    try {
//                        job.restart();
//                    } catch (Exception e) {
//                        throw new AssertionError("Could not restart job", e);
//                    }
//                }, action.restart, TimeUnit.MILLISECONDS);
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
}
