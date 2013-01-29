/*
 * This file is part of experimaestro.
 * Copyright (c) 2013 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.scheduler;

import bpiwowar.argparser.utils.ReadLineIterator;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.UnlockableException;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;

import static sf.net.experimaestro.scheduler.Resource.LOCK_EXTENSION;
import static sf.net.experimaestro.scheduler.Resource.STATUS_EXTENSION;

/**
 * One can write, many can read
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 29/1/13
 */
public class ReadWriteDependency extends Dependency {
    static final private Logger LOGGER = Logger.getLogger();

    protected ReadWriteDependency() {

    }

    public ReadWriteDependency(long from) {
        super(from);
    }


    @Override
    synchronized protected DependencyStatus _accept(Scheduler scheduler, Resource from) {
        Resource resource = getFrom(scheduler, from);
        resource.init(scheduler);
        final FileObject file;
        try {
            file = resource.getFileWithExtension(LOCK_EXTENSION);
            return file.exists() ? DependencyStatus.WAIT : DependencyStatus.OK_LOCK;
        } catch (FileSystemException e) {
            LOGGER.error(e, "Error while checking the presence of lock file for [%s]", from);
            return DependencyStatus.ERROR;

        }

    }

    @Override
    synchronized protected Lock _lock(Scheduler scheduler, Resource from, String pid) throws UnlockableException {
        Resource resource = getFrom(scheduler, from);
        try {
            FileObject file = resource.getFileWithExtension(LOCK_EXTENSION);
            final Lock lockFile = resource.getMainConnector().createLockFile(file.getName().getPath());
            return lockFile;
        } catch (FileSystemException e) {
            throw new UnlockableException(e);
        }
    }


    /**
     * This data will be stored with the resource
     */
    static public class Data extends LockData {
        int writers = 0;
        int readers = 0;
        long lastUpdate = 0;

        /**
         * Update the state file
         *
         * @param pidFrom     The old PID (or 0 if none)
         * @param pidTo       The new PID (or 0 if none)
         * @param writeAccess True if we need the write access
         * @throws UnlockableException
         */
        public void updateStatusFile(SingleHostConnector connector, String path, String pidFrom, String pidTo, boolean writeAccess)
                throws UnlockableException {
            // --- Lock the resource
            try (Lock fileLock = connector.createLockFile(path + LOCK_EXTENSION)) {

                try {
                    // --- Read the resource state

                    if (pidFrom == null) {
                        final FileObject tmpFile = connector.resolveFile(path + STATUS_EXTENSION);
                        PrintWriter out = new PrintWriter(tmpFile.getContent().getOutputStream(true));
                        // We are adding a new entry
                        out.format("%s %s%n", pidTo, writeAccess ? "w" : "r");
                        out.close();
                    } else {
                        final FileObject tmpFile = connector.resolveFile(path + STATUS_EXTENSION + ".tmp");
                        PrintWriter out = new PrintWriter(tmpFile.getContent().getOutputStream());
                        // We are modifying an entry: rewrite the file
                        Map<String, Boolean> processMap = new TreeMap<>();
                        updateFromStatusFile(connector, path, processMap);
                        processMap.remove(pidFrom);
                        if (pidTo != null)
                            processMap.put(pidTo, writeAccess);
                        for (Map.Entry<String, Boolean> x : processMap.entrySet())
                            out.format("%s %s%n", x.getKey(), x.getValue() ? "w" : "r");
                        out.close();
                        tmpFile.moveTo(connector.resolveFile(path + STATUS_EXTENSION));
                    }

                    if (writeAccess)
                        writers += 1;
                    else
                        readers += 1;
                } catch (Exception e) {
                    throw new UnlockableException(
                            "Status file '%s' could not be created", path + STATUS_EXTENSION);
                }
            }
        }


        /**
         * Update the state of the resource using its state file
         *
         * @return A boolean specifying whether something was updated
         * @throws java.io.FileNotFoundException If some error occurs while reading state
         */
        public void updateFromStatusFile(SingleHostConnector connector, String path, Map<String, Boolean> map) throws Exception {
            final FileObject statusFile = connector.resolveFile(path + STATUS_EXTENSION);

            if (!statusFile.exists()) {
                writers = readers = 0;
                return;
            } else {
                // Check if we need to read the file
                long lastModified = statusFile.getContent().getLastModifiedTime();
                if (lastUpdate >= lastModified)
                    return;

                lastUpdate = lastModified;

                try {
                    for (String line : new ReadLineIterator(statusFile.getContent().getInputStream())) {
                        String[] fields = line.split("\\s+");
                        if (fields.length != 2)
                            LOGGER.error(
                                    "Skipping line %s (wrong number of fields)",
                                    line);
                        else {
                            if (fields[1].equals("r"))
                                readers += 1;
                            else if (fields[1].equals("w"))
                                writers += 1;
                            else
                                LOGGER.error("Skipping line %s (unkown mode %s)",
                                        fields[1]);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }
}
