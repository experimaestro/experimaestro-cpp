package sf.net.experimaestro.locks;

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

import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.utils.log.Logger;

import javax.persistence.Entity;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static java.lang.String.format;


/**
 * A simple file lock for the resource
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Entity
public class FileLock extends Lock {

    final static private Logger LOGGER = Logger.getLogger();

    /**
     * Lock
     */
    Path lockFile;

    /**
     * Used for (de)serialization
     */
    protected FileLock() {
    }


    @Override
    public String toString() {
        return "FileLock{" +
                "lockFile=" + lockFile +
                '}';
    }

    /**
     * Lock a file. If the file exists, waits for it to be deleted.
     *
     * @param lockPath
     * @throws IOException
     */
    public FileLock(Path lockPath, boolean wait) throws LockException {
        this.lockFile = lockPath;
        try {
            while (true) {
                try {
                    // FIXME HACK - true hack
                    if (Files.exists(lockPath))
                        throw new FileAlreadyExistsException(format("Lock file %s exists", lockFile));
                    Files.createFile(lockPath);
                    LOGGER.debug("Created lock file %s", lockFile);
                    break;
                } catch (FileAlreadyExistsException e) {
                    if (!wait) {
                        throw new LockException("The lock file %s already exists", lockPath);
                    }

                    try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
                        lockPath.getParent().register(watcher, StandardWatchEventKinds.ENTRY_DELETE);
                        LOGGER.debug("Waiting for lock file %s", lockFile);
                        watcher.take();
                    } catch (NoSuchFileException f) {
                        // file was deleted before we started to monitor it
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new LockException(e, "Could not create the lock file");
        }
    }

    public FileLock(File lockFile) throws LockException {
        this(lockFile.toPath(), true);
    }

    public FileLock(String lockFile) throws LockException {
        this(new File(lockFile).toPath(), true);
    }

    public FileLock(String lockFile, boolean wait) throws LockException {
        this(new File(lockFile).toPath(), wait);
    }

    /*
     * (non-Javadoc)
     *
     * @see bpiwowar.expmanager.rsrc.Lock#close()
     */

    public void close() {
        if (lockFile != null && Files.exists(lockFile)) {
            boolean success = false;
            try {
                success = Files.deleteIfExists(lockFile);
            } catch (IOException e) {
                LOGGER.error(e);
            }
            lockFile = null;
            if (!success) {
                LOGGER.warn("Could not delete lock file %s", lockFile);
            } else {
                LOGGER.debug("Deleted lock file %s", lockFile);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public void changeOwnership(String pid) {
        // TODO Auto-generated method stub
    }

}