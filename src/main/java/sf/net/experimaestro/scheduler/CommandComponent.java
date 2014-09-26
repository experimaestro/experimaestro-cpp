package sf.net.experimaestro.scheduler;

import com.sleepycat.persist.model.Persistent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.utils.log.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by bpiwowar on 26/9/14.
 */
public interface CommandComponent {
    final static Logger LOGGER = Logger.getLogger();

    /**
     * Returns the path to the file of this component
     *
     * @param environment     Binds identifiers to file objects
     * @return A string representing the path to the file for this component
     * @throws org.apache.commons.vfs2.FileSystemException
     */
    java.lang.String prepare(CommandEnvironment environment) throws IOException;

    @Persistent
    class CommandOutput implements CommandComponent {
        /**
         * The output
         */
        Command command;

        /**
         * The object (or null if standard output)
         */
        String path;

        public CommandOutput(Command command, String path) {
            this.command = command;
            this.path = path;
        }

        @Override
        public java.lang.String prepare(CommandEnvironment environment) throws FileSystemException {
            throw new NotImplementedException();
        }
    }

    @Persistent
    class String implements CommandComponent {
        java.lang.String string;

        private String() {
        }

        public String(java.lang.String string) {
            this.string = string;
        }

        @Override
        public java.lang.String prepare(CommandEnvironment environment) {
            return string;
        }

        @Override
        public java.lang.String toString() {
            return string;
        }
    }

    @Persistent
    class Path implements CommandComponent {

        private java.lang.String filename;

        private Path() {
        }

        public Path(FileObject file) {
            filename = file.getName().getPath();
        }

        public Path(java.lang.String filename) {
            this.filename = filename;
        }

        @Override
        public java.lang.String prepare(CommandEnvironment environment) throws FileSystemException {
            FileObject object = Scheduler.getVFSManager().resolveFile(filename);
            return environment.resolve(object);
        }

        @Override
        public java.lang.String toString() {
            return java.lang.String.format("<xp:path>%s</xp:path>", filename);
        }
    }

    @Persistent
    class ParameterFile implements CommandComponent {
        java.lang.String key;
        byte[] content;

        private ParameterFile() {
        }

        public ParameterFile(java.lang.String key, byte[] content) {
            this.key = key;
            this.content = content;
        }

        @Override
        public java.lang.String prepare(CommandEnvironment environment) throws IOException {
            FileObject file = environment.getAuxiliaryFile(key, ".input");
            OutputStream out = file.getContent().getOutputStream();
            out.write(content);
            out.close();

            return environment.resolve(file);
        }

        @Override
        public java.lang.String toString() {
            return java.lang.String.format("ParameterFile(%s)", key);
        }
    }
}
