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

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * This class represents any layer that can get between a host where files can be stored
 * and possibly where a command can be executed.
 *
 * Connectors are stored in the database so that they can be used
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/6/12
 */
@Entity
public abstract class Connector implements Comparable<Connector> {
    /**
     * Each connector has a unique integer ID
     */
    @PrimaryKey(sequence = "key")
    long key;


    /**
     * A URL type secondary identifier in order to find matching connectors.
     */
    @SecondaryKey(relate = Relationship.ONE_TO_MANY)
    private String identifier;

    public Connector(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Retrieves a connector with some requirements
     * @return A valid connector or null if no connector meet the requirements
     */
    public abstract SingleHostConnector getConnector(ComputationalRequirements requirements);

    /**
     * Returns the main connector for this group
     * @return A valid single host connector
     */
    public abstract SingleHostConnector getMainConnector();


    protected Connector() {
    }


    /**
     * Returns true if the connector can execute commands
     */
    public boolean canExecute() {
        return false;
    }

    /**
     * Returns the connectorId key
     */
    public final long getKey() {
        return key;
    }


//    /**
//     * Create a file with a thread safe mechanism
//     *
//     * @param path
//     * @return A lock object
//     * @throws UnlockableException
//     */
//    public abstract Lock createLockFile(String path) throws UnlockableException;
//


    @Override
    final public int compareTo(Connector connector) {
        return Long.compare(key, connector.key);
    }

    public ConnectorDelegator delegate() {
        return new ConnectorDelegator(this);
    }

    /**
     * Return a new connector from an URI
     */
    public static Connector create(String uriString, ConnectorOptions options) throws URISyntaxException {
        return create(new URI(uriString), options);
    }

    public static Connector create(URI uri, ConnectorOptions options) {
        switch (uri.getScheme()) {
            case "ssh":
                return new SSHConnector(uri, options);
            case "local":
                return new LocalhostConnector();
            default:
                throw new IllegalArgumentException("Unknown connector scheme: " + uri.getScheme());
        }
    }

}
