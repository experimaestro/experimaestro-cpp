/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.locks;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.Persistent;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;


/**
 * A simple file lock for the resource
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Persistent
public class FileLock implements Lock {

    final static private Logger LOGGER = Logger.getLogger();

    /**
     * Lock
     */
    File lockFile;

    /**
     * Used for (de)serialization
     */
    private FileLock() {
    }

    /**
     * Lock a file. If the file exists, waits for it to be deleted.
     *
     * @param lockFile
     * @throws IOException
     */
    public FileLock(File lockFile, boolean wait) throws LockException {
        this.lockFile = lockFile;

        // FIXME: this is not reliable... but we rely on it for the moment!
        try {
            while (!lockFile.createNewFile()) {
                try (WatchService watcher = FileSystems.getDefault().newWatchService()) {

                    Path path = Paths.get(lockFile.toURI());

                    WatchKey key = path.register(watcher, StandardWatchEventKinds.ENTRY_DELETE);
                    if (wait)
                        watcher.take();
                    else throw new LockException();
                    lockFile.deleteOnExit();

                } catch (java.nio.file.NoSuchFileException e) {
                    // file was deleted before we started to monitor it
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new LockException(e, "Could not create the lock file");
        }
    }

    public FileLock(File lockFile) throws LockException {
        this(lockFile, true);
    }

    public FileLock(String lockFile) throws LockException {
        this(new File(lockFile), true);
    }

    public FileLock(String lockFile, boolean wait) throws LockException {
        this(new File(lockFile), wait);
    }

    /*
     * (non-Javadoc)
     *
     * @see bpiwowar.expmanager.rsrc.Lock#close()
     */

    public void close() {
        if (lockFile != null && lockFile.exists()) {
            boolean success = lockFile.delete();
            lockFile = null;
            if (!success)
                LOGGER.warn("Could not delete lock file %s", lockFile);
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

    @Override
    public void init(Scheduler scheduler) throws DatabaseException {
    }

}