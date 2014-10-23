/*
 * This file is part of experimaestro.
 * Copyright (c) 2013 B. Piwowarski <benjamin@bpiwowar.net>
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

import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.connectors.XPMProcess;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.ThreadCount;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A job that just waits a bit
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 30/1/13
 */
public class WaitingJob extends Job {
    final static private Logger LOGGER = Logger.getLogger();
    private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    transient private ThreadCount counter;

    // When was the job ready to run
    long readyTimestamp = 0;

    // id for debugging
    private String id;

    /**
     * Initialisation of a task
     * <p/>
     * The job is by default initialized as "WAITING": its state should be updated after
     * the initialization has finished
     *
     * @param connector
     * @param path
     */
    public WaitingJob(Connector connector, Path path) {
        super(connector, path);
    }


    static public class Action {
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
    // The code to return

    ArrayList<Action> actions;
    int currentIndex;
    transient Action current;

    public WaitingJob() {
    }

    public WaitingJob(ThreadCount counter, File dir, String id, Action... actions) {
        super(LocalhostConnector.getInstance(), new File(dir, id).toPath());
        this.counter = counter;
        this.id = id;
        counter.add(actions.length);

        this.actions = new ArrayList<>(Arrays.asList(actions));
        currentIndex = 0;

        // put ourselves in waiting mode (rather than ON HOLD default)
        this.setState(ResourceState.WAITING);
    }

    public void restart(Action... actions) throws Exception {
        this.actions = new ArrayList<>(Arrays.asList(actions));
        currentIndex = 0;
        restart();
    }


    public void init(Scheduler scheduler) {
        // FIXME: does nothing now...
        current = currentIndex < actions.size() ? actions.get(currentIndex) : null;
    }

    @Override
    protected XPMProcess startJob(ArrayList<Lock> locks) throws Throwable {
        assert readyTimestamp > 0;
        if (currentIndex >= actions.size()) {
            throw new AssertionError("No next action");
        }

        current = actions.get(currentIndex++);
        return new MyXPMProcess(counter, getMainConnector(), this, current);
    }

    @Override
    public boolean canBeOverriden(Resource current) {
        // We don't want to be overriden
        return false;
    }


    @Override
    public boolean setState(ResourceState state) {
        if (state == ResourceState.READY)
            readyTimestamp = System.currentTimeMillis();

        ResourceState oldState
                = getState();
        if (!super.setState(state))
            return false;

        boolean old_active = oldState != null && oldState.isActive();
        LOGGER.debug("State going from %s [%b] to %s [%b] for job %s", oldState, old_active, state, state.isActive(), id);

        // If we go to a non active state, release the counter
        if (old_active && !state.isActive() && counter != null) {
            LOGGER.debug("Releasing counter for job %s : counter=%d", id, counter.getCount() - 1);
            counter.del();
            if (state == ResourceState.ERROR || state == ResourceState.DONE) {
                if (current.restart > 0) {
                    if (currentIndex < actions.size())
                        scheduler.schedule(new Runnable() {
                            @Override
                            public void run() {
                                LOGGER.debug("Restarting job %s[%d]", id);
                                try {
                                    restart();
                                } catch (Exception e) {
                                    throw new AssertionError("Could not restart job", e);
                                }
                            }
                        }, current.restart, TimeUnit.MILLISECONDS);
                    else {
                        LOGGER.error("Not restarting since there is no next action");
                    }
                }
            }
        }


        return true;
    }

    @Override
    protected synchronized boolean doUpdateStatus(boolean store) throws Exception {
        final boolean b = super.doUpdateStatus(store);
        return b;
    }

    public int finalCode() {
        return actions.get(actions.size() - 1).code;
    }


    static private class MyXPMProcess extends XPMProcess {
        private long timestamp;
        private transient ThreadCount counter;
        Action action;

        public MyXPMProcess() {
            LOGGER.debug("Default constructor for MyXPMProcess");
        }

        @Override
        public void init(Job job) {
            LOGGER.debug("Initialized with job %s", job);
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
