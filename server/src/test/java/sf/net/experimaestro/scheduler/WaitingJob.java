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
import sf.net.experimaestro.utils.ThreadCount;

import javax.persistence.Entity;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import static sf.net.experimaestro.scheduler.ResourceState.*;

/**
 * Extends Job to collect some information for testing purposes
 */
@Entity
public class WaitingJob extends Job {
    /**
     * The statuses of the different jobs
     */
    static ArrayList<Status> statuses = new ArrayList<>();
    /**
     * The list of actions
     */
    ArrayList<WaitingJobRunner.Action> actions;

    /* The index in the static status list */
    int statusIndex;

    /* id for debugging */
    private String debugId;

    protected WaitingJob() {}

    public WaitingJob(ThreadCount counter, File dir, String debugId, WaitingJobRunner.Action... actions) {
        super(LocalhostConnector.getInstance(), new File(dir, debugId).toPath());


        jobRunner = new WaitingJobRunner();

        Status status = new Status();
        synchronized (statuses) {
            statusIndex = statuses.size();
            statuses.add(status);
        }
        setJobRunner(jobRunner);
        status.counter = counter;
        this.debugId = debugId;
        counter.add(actions.length);

        this.actions = new ArrayList<>(Arrays.asList(actions));
        status.currentIndex = 0;

        // put ourselves in waiting mode (rather than ON HOLD default)
        setState(WAITING);
    }

    @Override
    public boolean setState(ResourceState state) {
        if (!super.setState(state)) {
            return false;
        }
        switch(state) {
            case READY:
                status().readyTimestamp = System.currentTimeMillis();
                break;
        }
        return true;
    }

    Status status() {
        return statuses.get(statusIndex);
    }

    public int finalCode() {
        return actions.get(status().currentIndex).code;
    }

    public void restart(WaitingJobRunner.Action action) {

    }

    public static class Status {
        // Current index (action to run)
        int currentIndex;

        // When was the job ready to run
        long readyTimestamp = 0;

        // Counter
        transient ThreadCount counter;
    }
}
