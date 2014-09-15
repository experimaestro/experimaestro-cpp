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

package sf.net.experimaestro.connectors;

import com.sleepycat.persist.model.Persistent;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.locks.Lock;

import static java.lang.String.format;

/**
 * A connector that corresponds to a single host.
 *
 * Descendant of this class of connector provide access to a file system and to a process launcher.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Persistent
abstract public class SingleHostConnector extends Connector implements Launcher {
    /**
     * Underlying filesystem
     */
    transient private FileSystem filesystem;

    public SingleHostConnector(String id) {
        super(id);
    }

    protected SingleHostConnector() {
    }


    @Override
    public SingleHostConnector getConnector(ComputationalRequirements requirements) {
        // By default, returns ourselves - TODO: check the requirements
        return this;
    }

    @Override
    public SingleHostConnector getMainConnector() {
        // By default, the main connector is ourselves
        return this;
    }


    /**
     * Get the underlying filesystem
     */
    protected abstract FileSystem doGetFileSystem() throws FileSystemException;

    /**
     * Get the underlying file system
     *
     * @return
     * @throws FileSystemException
     */
    public FileSystem getFileSystem() throws FileSystemException {
        if (filesystem == null)
            return filesystem = doGetFileSystem();
        return filesystem;
    }


    /**
     * Resolve the file name
     *
     * @param path The path to the file
     * @return A file object
     * @throws FileSystemException
     */
    public FileObject resolveFile(String path) throws FileSystemException {
        return getFileSystem().resolveFile(path);
    }

    /**
     * Resolve a FileObject to a local path
     *
     * Throws an exception when the file name cannot be resolved, i.e. when
     * the file object is not
     */
    public String resolve(FileObject file) throws FileSystemException {
        if (file.getFileSystem() != this.getFileSystem()) {
            throw new FileSystemException("Cannot resolve %s from %s", file, this);
        }

        return file.getName().getPath();
    }

    public String resolve(String path) throws FileSystemException {
        return resolve(resolveFile(path));
    }


    /** Returns a process builder */
    public abstract XPMProcessBuilder processBuilder();

    /** Lock a file */
    public abstract Lock createLockFile(String path, boolean wait) throws LockException;

    /** Returns the hostname */
    public abstract String getHostName();

    @Override
    public XPMProcessBuilder processBuilder(SingleHostConnector connector) {
        if (connector != this)
            throw new ExperimaestroRuntimeException("");

        return this.processBuilder();
    }

    static private final char[] chars = new String("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789").toCharArray();
    public FileObject getTemporaryFile(String prefix, String suffix) throws FileSystemException {
        FileObject tmpdir = getTemporaryDirectory();

        final int MAX_ATTEMPTS = 1000;

        for(int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            FileObject child = tmpdir.resolveFile(prefix + RandomStringUtils.random(10, chars) + suffix);
            if (!child.exists()) {
                try {
                    child.createFile();
                } catch(Throwable t) {
                }
                return child;
            }
        }
        throw new FileSystemException(format("Could not find a proper temporary file name after %d attempts", MAX_ATTEMPTS));
    }

    protected abstract FileObject getTemporaryDirectory() throws FileSystemException;
}
