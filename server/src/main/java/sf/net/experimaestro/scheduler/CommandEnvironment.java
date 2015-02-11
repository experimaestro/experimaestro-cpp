package sf.net.experimaestro.scheduler;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.utils.IdentityHashSet;
import sf.net.experimaestro.utils.log.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;

import static java.lang.String.format;

/**
 * Command launching environment.
 *
 * The command environment is used when running a command to keep
 * track of all the data necessary for launching it.
 */
public abstract class CommandEnvironment implements Closeable {
    private final static Logger LOGGER = Logger.getLogger();

    /**
     * The host where the command is executed
     */
    protected final SingleHostConnector connector;

    /**
     * The auxiliary files created during the command launch
     */
    protected ArrayList<FileObject> files = new ArrayList<>();

    /**
     * Count for unique file names
     */
    private int uniqueCount;

    /**
     * Auxiliary data stored during launch
     */
    IdentityHashMap<Object, Object> data = new IdentityHashMap<>();

    /**
     * Commands that should be run in detached mode
     */
    IdentityHashSet<Command> detached = new IdentityHashSet<>();

    public CommandEnvironment(SingleHostConnector connector) {
        this.connector = connector;
    }

    public Object getData(Object key) {
        return data.get(key);
    }

    public Object setData(Object key, Object value) {
        return data.put(key, value);
    }

    public String resolve(FileObject file) throws FileSystemException {
        return connector.resolve(file);
    }

    abstract FileObject getAuxiliaryFile(String prefix, String suffix) throws FileSystemException;

    abstract public String getWorkingDirectory() throws FileSystemException;

    /**
     * Get a unique file name using a counter
     * @param prefix The prefix
     * @param suffix The suffix
     * @return The file object
     * @throws FileSystemException If a problem occurs while creating the file
     */
    FileObject getUniqueFile(String prefix, String suffix) throws FileSystemException {
        return getAuxiliaryFile(format("%s-%04d", prefix, uniqueCount++), suffix);
    }


    public boolean detached(Command command) {
        return detached.contains(command);
    }

    public void detached(Command command, boolean value) {
        if (value) detached.add(command);
        else detached.remove(command);
    }

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

    /**
     * A folder-based environment.
     *
     * Will persist after the command has run.
     */
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
