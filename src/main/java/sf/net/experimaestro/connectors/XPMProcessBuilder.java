package sf.net.experimaestro.connectors;

import org.apache.commons.vfs2.FileObject;
import sf.net.experimaestro.exceptions.LaunchException;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.utils.arrays.ListAdaptator;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * This class is used to create processes on a given host through a
 * specific connection method (e.g. direct, ssh)
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

    protected Redirect input;
    protected Redirect output;
    protected Redirect error;

    /** Whether this process should be bound to the Java process */
    private boolean detach;

    public XPMProcessBuilder command(List<String> command) {
        this.command = command;
        return this;
    }

    public XPMProcessBuilder command(String... command) {
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

    /** Start the process and return an Experimaestro process */
    abstract public XPMProcess start() throws LaunchException, IOException;


    /**
     * Represents a process source of input or output
     */
    static public class Redirect {
        private FileObject file;
        private Type type;

        public static final Redirect PIPE = new Redirect(Type.PIPE, null);
        public static final Redirect INHERIT = new Redirect(Type.INHERIT, null);

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

        public Type type() {
            return type;
        }

        static public enum Type {
            READ, APPEND, WRITE, PIPE, INHERIT;

            public boolean isReader() {
                return this == READ;
            }

            public boolean isWriter() {
                return this == APPEND || this == WRITE;
            }
        }

        static Redirect from(FileObject file) {
            return new Redirect(Type.READ, file);
        }

        static Redirect append(FileObject file) {
            return new Redirect(Type.APPEND, file);
        }

        static Redirect to(FileObject file) {
            return new Redirect(Type.WRITE, file);
        }
    }
}
