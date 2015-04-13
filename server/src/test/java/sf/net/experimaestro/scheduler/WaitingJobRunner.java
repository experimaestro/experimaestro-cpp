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

import javax.persistence.Entity;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * A job that just waits a bit
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Entity
public class WaitingJobRunner extends JobRunner {
    final static private Logger LOGGER = Logger.getLogger();

    public WaitingJobRunner() {
    }


    public WaitingJob.Status status() {
        return WaitingJob.statuses.get(((WaitingJob)job).statusIndex);
    }

    @Override
    public XPMProcess prepareJob(ArrayList<Lock> locks) {
        WaitingJob.Status status = status();
        assert status.readyTimestamp > 0;
        if (status.currentIndex >= ((WaitingJob)job).actions.size()) {
            throw new AssertionError("No next action");
        }

        final WaitingJobProcess.Action action = ((WaitingJob)job).actions.get(status.currentIndex);
        return new WaitingJobProcess(job.getMainConnector(), job, action);
    }

    @Override
    Path outputFile(Job job) throws FileSystemException {
        return null;
    }

    @Override
    public Stream<Dependency> dependencies() {
        return Stream.of();
    }

}
