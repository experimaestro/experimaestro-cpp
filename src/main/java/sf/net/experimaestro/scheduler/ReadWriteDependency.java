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
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.Persistent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.locks.Lock;
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
@Persistent
public class ReadWriteDependency extends Dependency {
    static final private Logger LOGGER = Logger.getLogger();

    protected ReadWriteDependency() {

    }

    public ReadWriteDependency(long from) {
        super(from);
    }

    @Override
    public String toString() {
        return "Read-Write";
    }

    @Override
    synchronized protected DependencyStatus _accept(Scheduler scheduler, Resource from) {
        // The file was generated, so it is just a matter of locking
        return DependencyStatus.OK_LOCK;
    }

    @Override
    synchronized protected Lock _lock(Scheduler scheduler, Resource from, String pid) throws LockException {
        // Retrieve data about resource
        Resource resource = getFrom(scheduler, from);

        return new StatusLock(resource, pid, false);
    }

    static private Data getData(Resource resource) {
        Data data = (Data) resource.getLockData();
        if (data == null) {
            resource.setLockData(data = new Data(resource));
        }
        return data;
    }

    @Persistent
    static public class StatusLock implements Lock {
        long resourceId;
        private String pid;

        transient Resource resource;

        protected StatusLock() {
        }

        public StatusLock(Resource resource, String pid, boolean writeAccess) throws LockException {
            this.resource = resource;
            this.pid = pid;
            this.resourceId = resource.getId();

            Data data = getData(resource);
            data.updateStatusFile(resource.getMainConnector(), resource.getLocator().getPath(), null, pid, writeAccess);
            LOGGER.debug("Created status lock [pid=%s] on %s: %s", pid, resource, data);
        }

        @Override
        public void close() {
            Data data = getData(resource);
            try {
                data.updateStatusFile(resource.getMainConnector(), resource.getLocator().getPath(), pid, null, /*not used*/false);
                LOGGER.debug("Removed status lock [pid=%s] on %s: %s", pid, resource, data);
            } catch (LockException e) {
                LOGGER.error(e, "Could not remove the status lock on %s", resource);
            }
        }

        @Override
        public void changeOwnership(String pid) throws LockException {
            Data data = getData(resource);
            data.updateStatusFile(resource.getMainConnector(), resource.getLocator().getPath(), this.pid, pid, /*not used*/false);
            LOGGER.debug("Changed status lock [pid=%s -> %s] on %s: %s", this.pid, pid, resource, data);
            this.pid = pid;
        }

        @Override
        public void init(Scheduler scheduler) throws DatabaseException {
            if (resource == null)
                resource = scheduler.getResource(resourceId);
        }
    }

    /**
     * This data is cached by the resource
     */
    static public class Data extends LockData {
        int writers = 0;
        int readers = 0;
        long lastUpdate = 0;

        Resource resource;

        public Data(Resource resource) {
            this.resource = resource;
        }

        public Lock lock(String pid, boolean writeAccess) throws FileSystemException, LockException {
            return new StatusLock(resource, pid, writeAccess);
        }

        @Override
        public String toString() {
            return String.format("Locks(r=%d/w=%d)", readers, writers);
        }

        /**
         * Update the state file
         *
         * @param pidFrom     The old PID (or 0 if none)
         * @param pidTo       The new PID (or 0 if none)
         * @param writeAccess True if we need the write access
         * @throws LockException
         */
        public void updateStatusFile(SingleHostConnector connector, String path, String pidFrom, String pidTo, boolean writeAccess)
                throws LockException {
            // --- Lock the resource
            try (Lock ignored = connector.createLockFile(path + LOCK_EXTENSION, true)) {

                try {
                    // --- Read the resource state

                    if (pidFrom == null) {
                        final FileObject tmpFile = connector.resolveFile(path + STATUS_EXTENSION);
                        PrintWriter out = new PrintWriter(tmpFile.getContent().getOutputStream(true));
                        // We are adding a new entry
                        out.format("%s %s%n", pidTo, writeAccess ? "w" : "r");
                        out.close();
                    } else {
                        Map<String, Boolean> processMap = new TreeMap<>();
                        updateFromStatusFile(connector, path, processMap);
                        if (pidFrom != null) {
                            Boolean writeAccessFrom = processMap.remove(pidFrom);
                            if (writeAccessFrom != null)
                                if (writeAccessFrom)
                                    writers--;
                                else
                                    readers--;

                        }

                        if (pidTo != null) {
                            processMap.put(pidTo, writeAccess);
                            if (writeAccess)
                                writers++;
                            else
                                readers++;

                        }


                        if (processMap.isEmpty()) {
                            connector.resolveFile(path + STATUS_EXTENSION).delete();
                        } else {
                            final FileObject tmpFile = connector.resolveFile(path + STATUS_EXTENSION + ".tmp");
                            PrintWriter out = new PrintWriter(tmpFile.getContent().getOutputStream());
                            // We are modifying an entry: rewrite the file
                            if (pidTo != null)
                                processMap.put(pidTo, writeAccess);
                            for (Map.Entry<String, Boolean> x : processMap.entrySet())
                                out.format("%s %s%n", x.getKey(), x.getValue() ? "w" : "r");
                            out.close();
                            tmpFile.moveTo(connector.resolveFile(path + STATUS_EXTENSION));
                        }
                    }

                } catch (Exception e) {
                    throw new LockException(e,
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


                try {
                    writers = readers = 0;
                    for (String line : new ReadLineIterator(statusFile.getContent().getInputStream())) {
                        String[] fields = line.split("\\s+");
                        if (fields.length != 2)
                            LOGGER.error(
                                    "Skipping line %s (wrong number of fields)",
                                    line);
                        else {
                            map.put(fields[0], fields[1].equals("w"));
                            if (fields[1].equals("r"))
                                readers += 1;
                            else if (fields[1].equals("w"))
                                writers += 1;
                            else
                                LOGGER.error("Skipping line %s (unkown mode %s)",
                                        fields[1]);
                        }
                    }

                    lastUpdate = lastModified;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }


    }
}
