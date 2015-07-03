package sf.net.experimaestro.connectors;

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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import sf.net.experimaestro.exceptions.LaunchException;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.utils.log.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Map;

import static sf.net.experimaestro.utils.JSUtils.unwrap;

/**
 * Created by bpiwowar on 26/9/14.
 */
public abstract class AbstractCommandBuilder {
    /**
     * The different input/output
     */
    protected AbstractProcessBuilder.Redirect input;
    protected AbstractProcessBuilder.Redirect output;
    protected AbstractProcessBuilder.Redirect error;

    /**
     * The associated job
     */
    Job job;

    /**
     * Working directory
     */
    Path directory;

    /**
     * Whether this process should be bound to the Java process
     */
    boolean detach;

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
     * @return A valid {@linkplain sf.net.experimaestro.connectors.XPMProcess} or null if fake is true
     * @param fake True if the process should not be started (but all files should be generated)
     */
    abstract public XPMProcess start(boolean fake) throws LaunchException, IOException;

    final public XPMProcess start() throws IOException, LaunchException { return start(false); }

    public AbstractCommandBuilder redirectError(AbstractProcessBuilder.Redirect destination) {
        if (!destination.isWriter())
            throw new IllegalArgumentException();
        this.error = destination;
        return this;
    }

    public AbstractCommandBuilder redirectInput(AbstractProcessBuilder.Redirect source) {
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
            redirectError(AbstractCommandBuilder.Redirect.PIPE);
            new Thread("stderr") {
                BufferedReader errorStream = new BufferedReader(new InputStreamReader(p.getErrorStream()));

                @Override
                public void run() {
                    errorStream.lines().forEach(line -> {
                        errLogger.info(line);
                    });
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

    /**
     * Represents a process source of input or output
     */
    static public class Redirect {
        public static final Redirect PIPE = new Redirect(Type.PIPE, null);
        /**
         * Indicates that subprocess I/O source or destination will be the
         * same as those of the current process.  This is the normal
         * behavior of most operating system command interpreters (shells).
         */
        public static final Redirect INHERIT = new Redirect(Type.INHERIT, null);
        private Path file;
        private String string;
        private Type type;

        private Redirect() {
            this.type = Type.INHERIT;
        }

        private Redirect(Type type, Path file) {
            this.type = type;
            this.file = file;
        }

        static public Redirect from(Path file) {
            return new Redirect(Type.READ, file);
        }

        static public Redirect append(Path file) {
            return new Redirect(Type.APPEND, file);
        }

        static public Redirect to(Path file) {
            return new Redirect(Type.WRITE, file);
        }

        public boolean isWriter() {
            return type.isWriter();
        }

        public Path file() {
            return file;
        }

        public String string() {
            return string;
        }

        public Type type() {
            return type;
        }

        static public enum Type {
            READ, APPEND, WRITE, PIPE, INHERIT;

            public boolean isReader() {
                return this == READ || this == INHERIT || this == PIPE;
            }

            public boolean isWriter() {
                return this == APPEND || this == WRITE || this == PIPE || this == INHERIT;
            }
        }
    }
}
