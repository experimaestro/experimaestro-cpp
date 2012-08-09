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
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.UnlockableException;
import sf.net.experimaestro.scheduler.Job;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

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
    @PrimaryKey(sequence = "identifier")
    long identifier;

    /**
     * Retrieves a connector with some requirements
     * @return A valid connector or null if no connector meet the requirements
     */
    public abstract Connector getConnector(ComputationalRequirements requirements);

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
     * Returns the connectorId identifier
     */
    public final long getIdentifier() {
        return identifier;
    }


    /**
     * Create a file with a thread safe mechanism
     *
     * @param path
     * @return A lock object
     * @throws UnlockableException
     */
    public abstract Lock createLockFile(String path) throws UnlockableException;




    /**
     * Execute a script or a command
     *
     * @param job        The job that will be notified when the process finishes (can be null)
     * @param command    The command to execute
     * @param locks      A set of locks that were taken before the process
     * @param detach     Should we detach the process or not
     * @param stdoutPath If not null, the standard output will be redirected to this file path
     * @param stderrPath If not null, the standard error will be redirected to this file path
     * @return A job monitor to control the execution of the command
     */
    public abstract sf.net.experimaestro.scheduler.Process
    exec(Job job, String command, ArrayList<Lock> locks, boolean detach,
         String stdoutPath, String stderrPath) throws Exception;


    @Override
    final public int compareTo(Connector connector) {
        return Long.compare(identifier, connector.identifier);
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
