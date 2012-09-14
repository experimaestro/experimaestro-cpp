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

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.UnlockableException;

/**
 * A fake connector used for internal purposes. It is backed up by
 * the temporary Apache VFS file system
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 15/8/12
 */
public class XPMConnector extends SingleHostConnector {
    final private static XPMConnector SINGLETON = new XPMConnector();

    protected XPMConnector() {
        super("xpm:");
    }

    @Override
    protected FileSystem doGetFileSystem() throws FileSystemException {
        throw new NotImplementedException();
    }

    @Override
    public XPMProcessBuilder processBuilder() {
        throw new NotImplementedException();
    }

    @Override
    public Lock createLockFile(String path) throws UnlockableException {
        throw new NotImplementedException();
    }

    @Override
    public String getHostName() {
        return "__xpm__";
    }

    public static Connector getInstance() {
        return SINGLETON;
    }

    @Override
    public XPMScriptProcessBuilder scriptProcessBuilder(SingleHostConnector connector, FileObject scriptFile) {
        throw new NotImplementedException();
    }
}
