package net.bpiwowar.xpm.commands;

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

import net.bpiwowar.xpm.connectors.XPMProcess;
import net.bpiwowar.xpm.exceptions.LaunchException;
import net.bpiwowar.xpm.exceptions.XPMRhinoException;
import net.bpiwowar.xpm.scheduler.Job;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Base class for command builders
 */
public abstract class AbstractCommandBuilder {
    /**
     * The different input/output
     */
    protected Redirect input;
    protected Redirect output;
    protected Redirect error;

    /**
     * The associated job
     */
    protected Job job;

    /**
     * Working directory
     */
    Path directory;

    /**
     * Whether this process should be bound to the Java process
     */
    protected boolean detach;

    /**
     * The environment
     */
    private Map<String, String> environment;

    public Map<String, String> environment() {
        return environment;
    }

    public void environment(Map<String, String> environment) {
        this.environment = environment;
    }

    public Path directory() {
        return directory;
    }

    public Job job() {
        return job;
    }

    public void job(Job job) {
        this.job = job;
    }


    public AbstractCommandBuilder directory(Path directory) {
        this.directory = directory;
        return this;
    }

    /**
     * Start the process and return an Experimaestro process
     *
     * @param fake True if the process should not be started (but all files should be generated)
     * @return A valid {@linkplain net.bpiwowar.xpm.connectors.XPMProcess} or null if fake is true
     */
    abstract public XPMProcess start(boolean fake) throws LaunchException, IOException;

    final public XPMProcess start() throws IOException, LaunchException {
        return start(false);
    }

    public AbstractCommandBuilder redirectError(Redirect destination) {
        if (!destination.isWriter())
            throw new IllegalArgumentException();
        this.error = destination;
        return this;
    }

    public AbstractCommandBuilder redirectInput(Redirect source) {
        if (!source.type.isReader())
            throw new IllegalArgumentException();
        this.input = source;
        return this;
    }

    public boolean detach() {
        return detach;
    }

    public AbstractCommandBuilder detach(boolean detach) {
        this.detach = detach;
        return this;
    }

    public AbstractCommandBuilder redirectOutput(Redirect destination) {
        if (!destination.isWriter())
            throw new IllegalArgumentException();
        this.output = destination;
        return this;
    }

    public String execute(Logger errLogger) throws IOException, LaunchException, InterruptedException {

        detach(false);

        XPMProcess p = start();

        if (errLogger != null) {
            redirectError(Redirect.PIPE);
            BufferedReader errorStream = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            new Thread("stderr") {
                @Override
                public void run() {
                    errorStream.lines().forEach(errLogger::info);
                }
            }.start();
        }

        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        int len = 0;
        char[] buffer = new char[8192];
        StringBuilder sb = new StringBuilder();
        while ((len = input.read(buffer, 0, buffer.length)) >= 0) {
            sb.append(buffer, 0, len);
        }
        input.close();

        int error = p.waitFor();
        if (error != 0) {
            errLogger.warn("Output was: %s", sb.toString());
            throw new XPMRhinoException("Command returned an error code %d", error);
        }
        return sb.toString();
    }

}