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

import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.connectors.XPMProcess;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.ThreadCount;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import static sf.net.experimaestro.scheduler.ResourceState.WAITING;
import static sf.net.experimaestro.utils.Functional.shouldNotThrow;

/**
 * Extends Job status collect some information for testing purposes
 */
@TypeIdentifier("WAITINGJOB")
public class WaitingJob extends Job {
    final static private Logger LOGGER = Logger.getLogger();

    static {
        Resources.REGISTRY.add(WaitingJob.class);
    }

    /**
     * The statuses of the different jobs
     */
    static ArrayList<Status> statuses = new ArrayList<>();
    /**
     * The list of actions
     */
    ArrayList<WaitingJobProcess.Action> actions;

    /* The index in the static status list */
    int statusIndex;

    /* id for debugging */
    private String debugId;

    public WaitingJob(Long id, String path) {
        super(id, path);
    }

    public WaitingJob(ThreadCount counter, File dir, String debugId, WaitingJobProcess.Action... actions) {
        super(LocalhostConnector.getInstance(), new File(dir, debugId).toString());


        Status status = new Status();
        synchronized (statuses) {
            statusIndex = statuses.size();
            statuses.add(status);
        }

        status.counter = counter;
        this.debugId = debugId;

        if (counter != null) {
            counter.add(actions.length);
        }

        this.actions = new ArrayList<>(Arrays.asList(actions));
        status.currentIndex = 0;

        // put ourselves in waiting mode (rather than ON HOLD default)
        shouldNotThrow(() -> super.setState(WAITING));

    }




    @Override
    public void save() throws SQLException {
        super.save();
        LOGGER.debug("Stored %s with state %s", this, getState());
    }

    @Override
    public boolean setState(ResourceState state) throws SQLException {
        ResourceState oldState = this.getState();
        final Status status = status();

        switch (state) {
            case READY:
                status.readyTimestamp = System.currentTimeMillis();
                break;
            case DONE:
            case ERROR:
            case ON_HOLD:
                break;
        }

        final boolean b = super.setState(state);

        if (status != null) {
            // Decrease the counter when the state is DONE or ERROR
            if (state.isUnactive() && !oldState.isUnactive()) {
                status.counter.del();
                final int count = status.counter.getCount();
                LOGGER.debug("Job %s went from %s to %s [counter = %d to %d]",
                        this, oldState, state, count + 1, count);
            }
            else if (!state.isUnactive() && oldState.isUnactive()) {
                status.counter.add();
                final int count = status.counter.getCount();
                LOGGER.debug("Job %s went from %s to %s [counter = %d to %d]",
                        this, oldState, state, count - 1, count);
            }

            // If we reached a final state
            if (state.isFinished()) {
                final int lockID = actions.get(status.currentIndex).removeLockID;
                if (lockID > 0) {
                    IntLocks.removeLock(lockID);
                }
            }

        }



        return b;
    }

    Status status() {
        if (statuses.isEmpty()) return null;
        return statuses.get(statusIndex);
    }

    public int finalCode() {
        return actions.get(status().currentIndex).code;
    }

    public void restart(WaitingJobProcess.Action action) {
        status().currentIndex = 0;
        actions.clear();
        actions.add(action);
        try {
            restart();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public long totalTime() {
        return actions.stream().mapToLong(a -> a.duration + a.restart).sum();
    }

    public static class Status {
        // Current index (action status run)
        int currentIndex;

        // When was the job ready status run
        long readyTimestamp = 0;

        // Counter
        transient ThreadCount counter;
    }


    @Override
    public XPMProcess start(ArrayList<Lock> locks, boolean fake) {
        if (fake) return null;

        WaitingJob.Status status = status();
        assert status.readyTimestamp > 0;
        if (status.currentIndex >= actions.size()) {
            throw new AssertionError("No next action");
        }

        final WaitingJobProcess.Action action = actions.get(status.currentIndex);
        return new WaitingJobProcess(getMainConnector(), this, action);
    }

    @Override
    public Path outputFile() throws FileSystemException {
        return null;
    }

    @Override
    public Stream<Dependency> dependencies() {
        return Stream.of();
    }

}
