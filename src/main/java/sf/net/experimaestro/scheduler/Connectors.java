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
import sf.net.experimaestro.connectors.Connector;
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
    }

    @Override
    protected boolean canOverride(Connector old, Connector connector) {
        return true;
    }

    @Override
    protected String getKey(Connector connector) {
        return new String(connector.getIdentifier());
    }

    @Override
    protected void init(Connector connector) {
        connector.init(scheduler);
    }

}
