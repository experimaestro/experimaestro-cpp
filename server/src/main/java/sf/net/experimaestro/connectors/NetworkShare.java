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

import sf.net.experimaestro.fs.XPMPath;
import sf.net.experimaestro.scheduler.Identifiable;
import sf.net.experimaestro.scheduler.Scheduler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Defines relationships between single host connectors and network shares
 */
final public class NetworkShare implements Identifiable {
    /** Internal ID */
    private long id;

    /** The possible accesses to this network share */
    private Collection<NetworkShareAccess> access;

    /**
     * The name of the logical host
     */
    String host;

    /**
     * The share name on that host
     */
    String name;

    public NetworkShare() {
    }

    public NetworkShare(String host, String share) {
        this.host = host;
        this.name = share;
        this.access = new ArrayList<>();
    }

    public Collection<NetworkShareAccess> getAccess() {
        return access;
    }

    public void add(NetworkShareAccess networkShareAccess) throws SQLException {
        // Save access
        networkShareAccess.save(this);

        // Add to ourselves
        access.add(networkShareAccess);
    }

    /**
     * Find a connector given its string ID
     *
     * @param host The host name
     * @param name The share name
     * @return The connector in database or null if none exist
     */
    public static NetworkShare find(String host, String name) throws SQLException {
        return Scheduler.get().shares().find(host, name);
    }

    /**
     * Find a network share for
     *
     * @param connector The single host connector
     * @param path      The network share path <code>share://host/root/path</code> path
     * @return
     */
    public static NetworkShareAccess find(SingleHostConnector connector, XPMPath path) throws SQLException {
        final NetworkShare networkShare = find(path.getHostName(), path.getShareName());
        if (networkShare == null) {
            return null;
        }

        for (NetworkShareAccess networkShareAccess : networkShare.getAccess()) {
            if (networkShareAccess.getConnector().getId() == connector.getId()) {
                return networkShareAccess;
            }
        }

        return null;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public void save() throws SQLException {
        Scheduler.get().networkShares().save(this);
    }

    public String getHost() {
        return host;
    }

    public String getName() {
        return name;
    }
}
