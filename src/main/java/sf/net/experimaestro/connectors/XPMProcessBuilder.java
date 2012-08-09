package sf.net.experimaestro.connectors;

import org.apache.commons.vfs2.FileObject;
import sf.net.experimaestro.utils.arrays.ListAdaptator;

import java.util.List;
import java.util.Map;

/**
 * This class is used to create processes on a given host through a
 * specific connection method (e.g. direct, ssh)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
abstract public class XPMProcessBuilder {
    /** The command to run */
    private List<String> command;

    /**  The environment */
    private Map<String, String> environment;

    private Redirect input;
    private Redirect output;
    private Redirect error;

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


    public List<String> command() {
        return command;
    }

    /** Start the process and return an Experimaestro process */
    abstract public XPMProcess start();


    /**
     * Represents a process source of input or output
     */
    static public class Redirect {
        private FileObject file;
        private Type type;

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
            READ, APPEND, WRITE;

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
