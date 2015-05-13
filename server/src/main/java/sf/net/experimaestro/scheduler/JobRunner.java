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

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Base class for all jobs
 */
public abstract class JobRunner {
    /** The associated job */
    transient Job job;

    /**
     * This is where the real job gets done
     *
     *
     * @param locks The locks that were taken
     * @param fake Use this to prepare everything without starting the process
     * @return The process corresponding status the job (or null if fake is true)
     * @throws Throwable If something goes wrong <b>before</b> starting the process. Otherwise, it should
     *                   return the process (unless fake is true)
     */
    public abstract XPMProcess start(ArrayList<Lock> locks, boolean fake) throws Exception;

    /**
     * Start a process
     * @param locks The locks that were taken
     * @return The process corresponding status the job
     * @throws Throwable If something goes wrong <b>before</b> starting the process. Otherwise, it should
     *                   return the process
     */
    public XPMProcess start(ArrayList<Lock> locks) throws Exception {
        return start(locks, false);
    }

    /**
     * Returns the output file
     * @param job
     * @return
     * @throws IOException If something goes wrong
     */
    abstract Path outputFile(Job job) throws IOException;

    public abstract Stream<Dependency> dependencies();
}
