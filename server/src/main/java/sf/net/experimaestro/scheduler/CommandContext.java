package sf.net.experimaestro.scheduler;

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

import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.utils.IdentityHashSet;
import sf.net.experimaestro.utils.log.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;

import static java.lang.String.format;

/**
 * The context of a command
 */
public abstract class CommandContext implements Closeable {
    static public class NamedPipeRedirections {
        public ArrayList<Path> outputRedirections = new ArrayList<>();
        public ArrayList<Path> errorRedirections = new ArrayList<>();
    }
    static private final NamedPipeRedirections EMPTY_REDIRECTIONS = new NamedPipeRedirections();

    private IdentityHashMap<AbstractCommand, NamedPipeRedirections> namedPipeRedirectionsMap
            = new IdentityHashMap<>();

    private final static Logger LOGGER = Logger.getLogger();

    /**
     * The host where the command is executed
     */
    protected final SingleHostConnector connector;

    /**
     * The auxiliary files created during the command launch
     */
    ArrayList<Path> files = new ArrayList<>();

    /**
     * Auxiliary data stored during launch
     */
    IdentityHashMap<Object, Object> data = new IdentityHashMap<>();

    /**
     * Commands that should be run in detached mode
     */
    IdentityHashSet<AbstractCommand> detached = new IdentityHashSet<>();

    /**
     * Count for unique file names
     */
    private int uniqueCount;

    public CommandContext(SingleHostConnector connector) {
        this.connector = connector;
    }

    public String resolve(Path file) throws FileSystemException {
        return connector.resolve(file);
    }

    abstract Path getAuxiliaryFile(String prefix, String suffix) throws IOException;

    abstract public String getWorkingDirectory() throws FileSystemException;



    public Object getData(Object key) {
        return data.get(key);
    }

    public Object setData(Object key, Object value) {
        return data.put(key, value);
    }


    /**
     * Get a unique file name using a counter
     * @param prefix The prefix
     * @param suffix The suffix
     * @return The file object
     * @throws FileSystemException If a problem occurs while creating the file
     */
    Path getUniqueFile(String prefix, String suffix) throws IOException {
        return getAuxiliaryFile(format("%s-%04d", prefix, uniqueCount++), suffix);
    }

    public NamedPipeRedirections getNamedRedirections(AbstractCommand key, boolean create) {
        NamedPipeRedirections x = namedPipeRedirectionsMap.get(key);
        if (x == null) {
            if (!create)
                return EMPTY_REDIRECTIONS;
            x = new NamedPipeRedirections();
            namedPipeRedirectionsMap.put(key, x);
        }
        return x;
    }


    public boolean detached(AbstractCommand command) {
        return detached.contains(command);
    }

    public void detached(AbstractCommand command, boolean value) {
        if (value) detached.add(command);
        else detached.remove(command);
    }


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

    /**
     * A folder-based environment.
     *
     * Will persist after the command has run.
     */
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
