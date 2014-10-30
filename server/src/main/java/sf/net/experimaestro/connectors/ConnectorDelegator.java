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

import sf.net.experimaestro.scheduler.Scheduler;

import javax.persistence.OneToOne;
import java.nio.file.FileSystemException;
import java.nio.file.Path;

/**
 * A connector delegator, for ease of use of connectors
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class ConnectorDelegator extends Connector {
    /**
     * The real connector
     */
    @OneToOne
    Connector connector;


    public ConnectorDelegator() {
    }

    public ConnectorDelegator(Connector connector) {
        super(connector.getIdentifier());
        this.connector = connector;
    }

    @Override
    public SingleHostConnector getConnector(ComputationalRequirements requirements) {
        return connector.getConnector(requirements);
    }

    @Override
    public SingleHostConnector getMainConnector() {
        return connector.getMainConnector();
    }

    @Override
    public Path resolve(String path) throws FileSystemException {
        return delegate().resolve(path);
    }

}
