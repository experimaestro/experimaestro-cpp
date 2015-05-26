package sf.net.experimaestro.connectors;

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

import org.apache.commons.lang.NotImplementedException;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.scheduler.TypeIdentifier;

import javax.persistence.Entity;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A fake connector used for internal purposes.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@TypeIdentifier("xpm")
public class XPMConnector extends SingleHostConnector {
    final private static XPMConnector SINGLETON = new XPMConnector();
    /**
     * A special connector for DB handled resources
     */
    private static final String ID = "xpmdb";

    protected XPMConnector() {
        super(ID + "://");
    }

    @Override
    public Path resolve(String path) {
        return Paths.get(path);
    }

    public static XPMConnector getInstance() {
        return SINGLETON;
    }

    @Override
    protected FileSystem doGetFileSystem() throws FileSystemException {
        throw new NotImplementedException();
    }

    @Override
    protected boolean contains(FileSystem fileSystem) {
        return false;
    }

    @Override
    public AbstractProcessBuilder processBuilder() {
        throw new NotImplementedException();
    }

    @Override
    public Lock createLockFile(Path path, boolean wait) throws LockException {
        throw new NotImplementedException();
    }

    @Override
    public String getHostName() {
        return "";
    }

    @Override
    protected Path getTemporaryDirectory() throws FileSystemException {
        throw new UnsupportedOperationException();
    }

    @Override
    public XPMScriptProcessBuilder scriptProcessBuilder(Path scriptFile) {
        throw new NotImplementedException();
    }
}
