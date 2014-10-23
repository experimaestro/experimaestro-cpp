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

import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.locks.Lock;

import javax.persistence.Entity;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.String.format;

/**
 * A connector that corresponds to a single host.
 * <p/>
 * Descendant of this class of connector provide access to a file system and to a process launcher.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Entity
abstract public class SingleHostConnector extends Connector {
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
    public Path resolveFile(String path) throws FileSystemException {
        return getFileSystem().getPath(path);
    }

    /**
     * Resolve a Path to a local path
     * <p/>
     * Throws an exception when the file name cannot be resolved, i.e. when
     * the file object is not
     */
    public String resolve(Path file) throws FileSystemException {
        if (!contains(file.getFileSystem())) {
            throw new FileSystemException(format("Cannot resolve file %s within filesystem %s", file, this));
        }

        return file.toString();
    }

    /**
     * Returns true if the filesystem matches
     */
    protected abstract boolean contains(FileSystem fileSystem) throws FileSystemException;

    /**
     * Returns a process builder
     */
    public abstract AbstractProcessBuilder processBuilder();

    /**
     * Lock a file
     */
    public abstract Lock createLockFile(Path path, boolean wait) throws LockException;

    /**
     * Returns the hostname
     */
    public abstract String getHostName();


    public Path getTemporaryFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(getTemporaryDirectory(), prefix, suffix);
    }

    protected abstract Path getTemporaryDirectory() throws FileSystemException;

    @Override
    public Path resolve(String path) throws FileSystemException {
        return getFileSystem().getPath(path);
    }

    /**
     * Creates a script builder
     * @param scriptFile The path to the script file to create
     * @return A builder
     * @throws FileSystemException if an exception occurs while accessing the script file
     */
    abstract public XPMScriptProcessBuilder scriptProcessBuilder(Path scriptFile)
            throws FileSystemException;
}
