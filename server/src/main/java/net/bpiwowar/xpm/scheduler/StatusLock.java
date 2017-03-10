package net.bpiwowar.xpm.scheduler;

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

import bpiwowar.argparser.utils.ReadLineIterator;
import net.bpiwowar.xpm.exceptions.LockException;
import net.bpiwowar.xpm.locks.FileLock;
import net.bpiwowar.xpm.locks.Lock;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.utils.FileNameTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.String.format;
import static net.bpiwowar.xpm.scheduler.Resource.LOCK_EXTENSION;
import static net.bpiwowar.xpm.scheduler.Resource.STATUS_EXTENSION;

/**
 * A lock managed by a file that records the reader and writers
 * of a resource
 */
@Exposed
@TypeIdentifier("status")
public class StatusLock extends Lock {
    final static private Logger LOGGER = LogManager.getFormatterLogger();

    Path path;

    /**
     * Number of writers
     */
    int writers = 0;

    /**
     * Number of readers
     */
    int readers = 0;

    /**
     * Time of last update
     */
    long lastUpdate = 0;

    /**
     * PID of our entry
     */
    private String pid;

    @Override
    protected void save(DatabaseObjects<Lock, Dependency> locks) throws SQLException {
        super.save(locks);
        saveShare(path);
    }


    public StatusLock(Path path, String pid, boolean writeAccess) throws LockException {
        this.path = path;
        this.pid = pid;

        updateStatusFile(null, pid, writeAccess);
        LOGGER.debug("Created status lock [pid=%s] on %s", pid, path);
    }

    public StatusLock(long id, Dependency dependency) {
        super(id);
    }

    @Override
    public void doClose() {
        try {
            updateStatusFile(pid, null, /*not used*/false);
            LOGGER.debug("Removed status lock [pid=%s] on %s", pid, path);
        } catch (LockException e) {
            LOGGER.error(() -> format("Could not remove the status lock on %s", path), e);
        }
    }

    @Override
    public void changeOwnership(String pid) throws LockException {
        updateStatusFile(this.pid, pid, /*not used*/false);
        LOGGER.debug("Changed status lock [pid=%s -> %s] on %s", this.pid, pid, path);
        this.pid = pid;
    }

    @Override
    public String toString() {
        return format("Locks(r=%d/w=%d)", readers, writers);
    }

    /**
     * Update the state file
     *
     * @param pidFrom     The old PID (or 0 if none)
     * @param pidTo       The new PID (or 0 if none)
     * @param writeAccess True if we need the write access
     * @throws LockException
     */
    public void updateStatusFile(String pidFrom, String pidTo, boolean writeAccess)
            throws LockException {
        // --- Lock the resource
        try (Lock ignored = new FileLock(LOCK_EXTENSION.transform(path), false)) {
            Path statusPath = STATUS_EXTENSION.transform(path);

            // --- Read the resource state

            if (pidFrom == null) {
                PrintWriter out = new PrintWriter(Files.newOutputStream(statusPath, StandardOpenOption.APPEND, StandardOpenOption.CREATE));
                // We are adding a new entry
                out.format("%s %s%n", pidTo, writeAccess ? "w" : "r");
                out.close();
            } else {
                Map<String, Boolean> processMap = new TreeMap<>();
                updateFromStatusFile(path, processMap);
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
                    if (Files.exists(statusPath)) {
                        Files.delete(statusPath);
                    }
                } else {
                    final Path tmpFile = new FileNameTransformer("", ".tmp").transform(statusPath);

                    PrintWriter out = new PrintWriter(Files.newOutputStream(tmpFile));
                    // We are modifying an entry: rewrite the file
                    if (pidTo != null)
                        processMap.put(pidTo, writeAccess);
                    for (Map.Entry<String, Boolean> x : processMap.entrySet())
                        out.format("%s %s%n", x.getKey(), x.getValue() ? "w" : "r");
                    out.close();

                    Files.deleteIfExists(statusPath);
                    Files.move(tmpFile, statusPath);
                }
            }

        } catch (Exception e) {
            throw new LockException(e, "Status file could not be created for %s", path);
        }
    }


    /**
     * Update the state of the resource using its state file
     *
     * @return A boolean specifying whether something was updated
     * @throws java.io.FileNotFoundException If some error occurs while reading state
     */
    public void updateFromStatusFile(Path path, Map<String, Boolean> map) throws Exception {
        final Path statusFile = STATUS_EXTENSION.transform(path);

        if (!Files.exists(statusFile)) {
            writers = readers = 0;
        } else {
            // Check if we need status read the file
            long lastModified = Files.getLastModifiedTime(statusFile).toMillis();
            if (lastUpdate >= lastModified)
                return;


            try {
                writers = readers = 0;
                for (String line : new ReadLineIterator(Files.newInputStream(statusFile))) {
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
