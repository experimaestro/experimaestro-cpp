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

import sf.net.experimaestro.connectors.XPMProcess;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.log.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.Serializable;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.ArrayList;
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
public class WaitingJobRunner extends JobRunner {
    final static private Logger LOGGER = Logger.getLogger();

    public WaitingJobRunner() {
    }


    public WaitingJob.Status status() {
        return WaitingJob.statuses.get(((WaitingJob)job).statusIndex);
    }

    @Override
    public XPMProcess startJob(ArrayList<Lock> locks) {
        WaitingJob.Status status = status();
        assert status.readyTimestamp > 0;
        if (status.currentIndex >= ((WaitingJob)job).actions.size()) {
            throw new AssertionError("No next action");
        }

        final Action action = ((WaitingJob)job).actions.get(status.currentIndex);
        return new WaitingJobProcess(status.counter, job.getMainConnector(), job, action);
    }

    @Override
    Path outputFile(Job job) throws FileSystemException {
        return null;
    }

    @Override
    public Stream<Dependency> dependencies() {
        return Stream.of();
    }

    public void restart(Action action) {
        throw new NotImplementedException();
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
}
