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
import sf.net.experimaestro.scheduler.Scheduler;

/**
 * A connector delegator, for ease of use of connectors
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 20/6/12
 */
@Persistent
public class ConnectorDelegator extends Connector {
    /** The real connector */
    transient Connector connector;


    public ConnectorDelegator() {
    }

    public ConnectorDelegator(Connector connector) {
        super(connector.getIdentifier());
        this.connector = connector;
    }

    public void init(Scheduler scheduler) throws DatabaseException {
        connector = scheduler.getConnector(identifier);
    }

    @Override
    public SingleHostConnector getConnector(ComputationalRequirements requirements) {
        return connector.getConnector(requirements);
    }

    @Override
    public SingleHostConnector getMainConnector() {
        return connector.getMainConnector();
    }

}
