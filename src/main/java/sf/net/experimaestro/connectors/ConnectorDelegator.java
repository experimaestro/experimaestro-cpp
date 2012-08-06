/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package sf.net.experimaestro.connectors;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.Persistent;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.UnlockableException;
import sf.net.experimaestro.scheduler.*;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * A connector delegator, for ease of use of connectors
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 20/6/12
 */
@Persistent
public class ConnectorDelegator extends Connector {
    transient Connector connector;

    public ConnectorDelegator() {
    }

    public ConnectorDelegator(Connector connector) {
        super(connector.getIdentifier());
        this.connector = connector;
    }

    void init(Scheduler scheduler) throws DatabaseException {
        connector = scheduler.getConnector(identifier);
    }

    @Override
    public sf.net.experimaestro.scheduler.Process exec(Job job, String command, ArrayList<Lock> locks, boolean detach, String stdoutPath, String stderrPath) throws Exception {
        return connector.exec(job, command, locks, detach, stdoutPath, stderrPath);
    }

    @Override
    public Lock createLockFile(String path) throws UnlockableException {
        return connector.createLockFile(path);
    }

    @Override
    protected FileSystem doGetFileSystem() throws FileSystemException {
        return connector.doGetFileSystem();
    }

}
