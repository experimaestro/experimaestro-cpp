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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.io.Serializable;

/**
 * An access to a network share
 */
@Entity
public class NetworkShareAccess implements Serializable {
    /**
     * The host that allows us to access the data
     */
    @Id
    @ManyToOne
    Connector connector;

    @Id
    @ManyToOne
    NetworkShare share;

    /**
     * Path on the single host connector
     */
    String path;


    /**
     * The priority of this access
     */
    int priority;

    public NetworkShareAccess(NetworkShare networkShare, SingleHostConnector connector, String path, int priority) {
        share = networkShare;
        this.connector = connector;
        this.path = path;
        this.priority = priority;
    }

    public NetworkShareAccess() {
    }

    public boolean is(SingleHostConnector connector) {
        return connector.equals(this.connector);
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
