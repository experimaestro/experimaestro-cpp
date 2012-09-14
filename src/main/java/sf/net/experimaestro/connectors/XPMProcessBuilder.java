/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.connectors;

import com.sleepycat.persist.model.Persistent;
import org.apache.commons.vfs2.FileObject;
import sf.net.experimaestro.exceptions.LaunchException;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.utils.arrays.ListAdaptator;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * This class is used to create processes on a given host through a
 * specific connection method (e.g. direct, ssh).
 *
 * This class borrows heavily from {@linkplain java.lang.ProcessBuilder},
 * with some differences:
 * <ol>
 *     <li>There is a parameter to detach the process (e.g. for SSH connections)</li>
 *     <li>A {@linkplain Job} can be associated to the process for notification</li>
 *     <li>A path should be associated (if detached) to the process</li>
 * </ol>
 *
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
abstract public class XPMProcessBuilder {
    /** The associated job */
    Job job;

    /** The command to run */
    private List<String> command;

    /**  The environment */
    private Map<String, String> environment;

    /** Working directory */
    String directory;

    /** The different input/output */
    protected Redirect input;
    protected Redirect output;
    protected Redirect error;

    /** Whether this process should be bound to the Java process */
    boolean detach;

    public XPMProcessBuilder command(List<String> command) {
        this.command = command;
        return this;
    }

    final public XPMProcessBuilder command(String... command) {
        return command(ListAdaptator.create(command));
    }

    public Map<String,String> environment() {
        return environment;
    }

    public void environment(Map<String,String> environment) {
        this.environment = environment;
    }

    public XPMProcessBuilder redirectOutput(Redirect destination) {
        if (!destination.isWriter())
            throw new IllegalArgumentException();
        this.output = destination;
        return this;
    }

    public XPMProcessBuilder redirectError(Redirect destination) {
        if (!destination.isWriter())
            throw new IllegalArgumentException();
        this.error = destination;
        return this;
    }

    public XPMProcessBuilder redirectInput(Redirect source) {
        if (!source.type.isReader())
            throw new IllegalArgumentException();
        this.input = source;
        return this;
    }


    public Job job() {
        return job;
    }

    public void job(Job job) {
        this.job = job;
    }

    public List<String> command() {
        return command;
    }

    public boolean detach() {
        return detach;
    }

    public void detach(boolean detach) {
        this.detach = detach;
    }

    public String directory() {
        return directory;
    }

    public XPMProcessBuilder directory(String directory) {
        this.directory = directory;
        return this;
    }

    /**
     *  Start the process and return an Experimaestro process
     *  @return A valid {@linkplain XPMProcess}
     */
    abstract public XPMProcess start() throws LaunchException, IOException;




    /**
     * Represents a process source of input or output
     */
    @Persistent
    static public class Redirect {
        private FileObject file;
        private String string;

        private Type type;

        public static final Redirect PIPE = new Redirect(Type.PIPE, null);
        public static final Redirect INHERIT = new Redirect(Type.INHERIT, null);

        private Redirect() {
            this.type = Type.INHERIT;
        }

        private Redirect(Type type, FileObject file) {
            this.type = type;
            this.file = file;
        }

        public boolean isWriter() {
            return type.isWriter();
        }

        public FileObject file() {
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

        static public Redirect from(FileObject file) {
            return new Redirect(Type.READ, file);
        }

        static public Redirect append(FileObject file) {
            return new Redirect(Type.APPEND, file);
        }

        static public Redirect to(FileObject file) {
            return new Redirect(Type.WRITE, file);
        }
    }
}
