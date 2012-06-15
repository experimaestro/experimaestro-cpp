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

package sf.net.experimaestro.scheduler;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.KeyField;
import com.sleepycat.persist.model.Persistent;
import sf.net.experimaestro.exceptions.ExperimaestroException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Identifies a resource on the network
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/6/12
 */
@Persistent
public class Locator implements Comparable<Locator> {
    @KeyField(value = 1)
    String connectorId;

    @KeyField(value = 2)
    String path;

    // TODO: we should use FileObject from Apache commons

    /**
     * Connector
     */
    transient Connector _connector;

    public Locator() {
    }

    public Locator(String connector, String path) {
        this.connectorId = connector;
        this.path = path;
    }

    public Locator(Locator identifier) {
        this.connectorId = identifier.connectorId;
        this.path = identifier.path;
    }

    void init(Scheduler scheduler) throws DatabaseException {
        _connector = scheduler.getConnector(connectorId);
    }

    public String getPath() {
        return path;
    }

    public Locator(Connector connector, String path) {
        this.connectorId = connector.getIdentifier();
        this.path = path;
        this._connector = connector;
    }

    @Override
    public int compareTo(Locator other) {
        int z = connectorId.compareTo(other.connectorId);
        if (z != 0) return z;
        return path.compareTo(path);
    }

    @Override
    public String toString() {
        return String.format("%s%s", connectorId, path);
    }

    public static Locator decode(String idString) {
        URI uri = null;
        try {
            uri = new URI(idString);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        if ("local".equals(uri.getScheme()))
            return LocalhostConnector.getIdentifier(uri);

        if ("ssh".equals(uri.getScheme()))
            return SSHConnector.getIdentifier(uri);

        throw new RuntimeException(String.format("Unknown scheme [%s] in URL [%s]", uri.getScheme(), idString));
    }


    public Connector getConnector() {
        if (_connector == null)
            throw new ExperimaestroException("The identifier has not been properly initialized");

        return _connector;
    }

    /**
     * Resolve a path relative to possibly relative to the identifier
     */
    public Locator resolvePath(String path, boolean parent) throws IOException {
        return new Locator(_connector, getConnector().resolvePath(this.path, path, parent));
    }

    public InputStream getInputStream() throws Exception {
        return getConnector().getInputStream(path);
    }
}
