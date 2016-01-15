package net.bpiwowar.xpm.commands;

import java.nio.file.Path;

/**
 * Represents a process source of input or output
 */
public class Redirect {
    /**
     * A pipe that will be processed by a Java process
     */
    public static final Redirect PIPE = new Redirect(Type.PIPE, null);

    /**
     * Indicates that subprocess I/O source or destination will be the
     * same as those of the current process.  This is the normal
     * behavior of most operating system command interpreters (shells).
     */
    public static final Redirect INHERIT = new Redirect(Type.INHERIT, null);

    private Path file;
    private String string;
    protected Type type;

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

    public enum Type {
        READ, APPEND, WRITE, PIPE, INHERIT;

        public boolean isReader() {
            return this == READ || this == INHERIT || this == PIPE;
        }

        public boolean isWriter() {
            return this == APPEND || this == WRITE || this == PIPE || this == INHERIT;
        }
    }
}
