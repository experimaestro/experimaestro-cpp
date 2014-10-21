package sf.net.experimaestro.scheduler;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.utils.log.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;

import static java.lang.String.format;

/**
 * Created by bpiwowar on 26/9/14.
 */
public abstract class CommandEnvironment implements Closeable {
    private final static Logger LOGGER = Logger.getLogger();

    /**
     * The host where the command is executed
     */
    protected final SingleHostConnector connector;

    ArrayList<FileObject> files = new ArrayList<>();


    public CommandEnvironment(SingleHostConnector connector) {
        this.connector = connector;
    }

    public String resolve(FileObject file) throws FileSystemException {
        return connector.resolve(file);
    }

    abstract FileObject getAuxiliaryFile(String prefix, String suffix) throws FileSystemException;

    abstract public String getWorkingDirectory() throws FileSystemException;

    /**
     * A temporary environment: all the auxiliary files will be deleted
     */
    static public class Temporary extends CommandEnvironment {
        public Temporary(SingleHostConnector connector) {
            super(connector);
        }

        @Override
        FileObject getAuxiliaryFile(String prefix, String suffix) throws FileSystemException {
            final FileObject temporaryFile = connector.getTemporaryFile(prefix, suffix);
            files.add(temporaryFile);
            return temporaryFile;
        }

        @Override
        public String getWorkingDirectory() {
            return null;
        }

        @Override
        public void close() throws IOException {
            for (FileObject file : files) {
                try {
                    file.delete();
                } catch (IOException e) {
                    LOGGER.error(e, "Could not delete %s", file);
                }
            }
        }
    }

    static public class FolderEnvironment extends CommandEnvironment {
        /**
         * The base name for generated files
         */
        private final String name;
        /**
         * The base folder for this process
         */
        FileObject folder;

        public FolderEnvironment(SingleHostConnector connector, FileObject basepath, String name) throws FileSystemException {
            super(connector);
            this.folder = basepath;
            this.name = name;
        }

        @Override
        FileObject getAuxiliaryFile(String prefix, String suffix) throws FileSystemException {
            return folder.resolveFile(format("%s.%s%s", name, prefix, suffix));
        }

        @Override
        public String getWorkingDirectory() throws FileSystemException {
            return connector.resolve(folder);
        }

        @Override
        public void close() throws IOException {
            // Keep all the files
        }
    }
}
