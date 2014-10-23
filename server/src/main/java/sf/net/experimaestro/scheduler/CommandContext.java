package sf.net.experimaestro.scheduler;

import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.utils.log.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static java.lang.String.format;

/**
 * The context of a command
 */
public abstract class CommandContext implements Closeable {
    private final static Logger LOGGER = Logger.getLogger();

    /**
     * The host where the command is executed
     */
    protected final SingleHostConnector connector;

    ArrayList<Path> files = new ArrayList<>();


    public CommandContext(SingleHostConnector connector) {
        this.connector = connector;
    }

    public String resolve(Path file) throws FileSystemException {
        return connector.resolve(file);
    }

    abstract Path getAuxiliaryFile(String prefix, String suffix) throws IOException;

    abstract public String getWorkingDirectory() throws FileSystemException;

    /**
     * A temporary environment: all the auxiliary files will be deleted
     */
    static public class Temporary extends CommandContext {
        public Temporary(SingleHostConnector connector) {
            super(connector);
        }

        @Override
        Path getAuxiliaryFile(String prefix, String suffix) throws IOException {
            final Path temporaryFile = connector.getTemporaryFile(prefix, suffix);
            files.add(temporaryFile);
            return temporaryFile;
        }

        @Override
        public String getWorkingDirectory() {
            return null;
        }

        @Override
        public void close() throws IOException {
            for (Path file : files) {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    LOGGER.error(e, "Could not delete %s", file);
                }
            }
        }
    }

    static public class FolderContext extends CommandContext {
        /**
         * The base name for generated files
         */
        private final String name;
        /**
         * The base folder for this process
         */
        Path folder;

        public FolderContext(SingleHostConnector connector, Path basepath, String name) throws FileSystemException {
            super(connector);
            this.folder = basepath;
            this.name = name;
        }

        @Override
        Path getAuxiliaryFile(String prefix, String suffix) throws FileSystemException {
            return folder.resolve(format("%s.%s%s", name, prefix, suffix));
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
