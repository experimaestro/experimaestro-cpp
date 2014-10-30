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

import javax.persistence.*;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * Base class for all jobs
 */
@Entity
public abstract class JobRunner {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(mappedBy = "jobRunner")
    Job job;

    /**
     * This is where the real job gets done
     *
     *
     * @param locks The locks that were taken
     * @return The process corresponding to the job
     * @throws Throwable If something goes wrong <b>before</b> starting the process. Otherwise, it should
     *                   return the process
     */
    public abstract XPMProcess startJob(ArrayList<Lock> locks) throws Exception;

    /**
     * Returns the output file
     * @param job
     * @return
     * @throws FileSystemException
     */
    abstract Path outputFile(Job job) throws FileSystemException;


    public long getId() {
        return id;
    }

    public abstract Stream<Dependency> dependencies();
}
