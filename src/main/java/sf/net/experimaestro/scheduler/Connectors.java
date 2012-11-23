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

package sf.net.experimaestro.scheduler;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.model.Persistent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.connectors.XPMProcessBuilder;
import sf.net.experimaestro.connectors.XPMScriptProcessBuilder;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.UnlockableException;
import sf.net.experimaestro.utils.log.Logger;

/**
 * Stores the connectors in a database
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Connectors extends CachedEntitiesStore<String, Connector> {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The associated scheduler
     */
    private final Scheduler scheduler;

    /**
     * A special connector for DB handled resources
     */
    public static final String XPM_CONNECTOR_ID = "xpmdb";


    /**
     * Initialise the set of connectors
     *
     * @param dbStore
     * @throws com.sleepycat.je.DatabaseException
     *
     */
    public Connectors(Scheduler scheduler, EntityStore dbStore)
            throws DatabaseException {
        super(dbStore.getPrimaryIndex(String.class, Connector.class));
        this.scheduler = scheduler;

        SingleHostConnector xpmConnector = new MySingleHostConnector();
        put(xpmConnector);
    }

    @Override
    protected boolean canOverride(Connector old, Connector connector) {
        return true;
    }

    @Override
    protected String getKey(Connector connector) {
        return connector.getIdentifier();
    }

    @Override
    protected void init(Connector connector) {
        connector.init(scheduler);
    }


    @Persistent
    private static class MySingleHostConnector extends SingleHostConnector {
        public MySingleHostConnector() {
            super(Connectors.XPM_CONNECTOR_ID);
        }

        @Override
        protected FileSystem doGetFileSystem() throws FileSystemException {
            throw new AssertionError();
        }

        @Override
        public XPMProcessBuilder processBuilder() {
            throw new AssertionError();
        }

        @Override
        public Lock createLockFile(String path) throws UnlockableException {
            return null;
        }

        @Override
        public String getHostName() {
            return "xpmdb";
        }

        @Override
        public XPMScriptProcessBuilder scriptProcessBuilder(SingleHostConnector connector, FileObject scriptFile) {
            throw new AssertionError();
        }
    }
}
