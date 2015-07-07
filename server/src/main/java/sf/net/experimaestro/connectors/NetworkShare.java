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

import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.fs.XPMPath;
import sf.net.experimaestro.scheduler.DatabaseObjects;
import sf.net.experimaestro.scheduler.Identifiable;
import sf.net.experimaestro.scheduler.Scheduler;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Defines relationships between single host connectors and network shares
 */
final public class NetworkShare implements Identifiable {
    /**
     * Internal ID
     */
    private long id;

    /**
     * The possible accesses to this network share
     */
    private Collection<NetworkShareAccess> access;

    /**
     * The name of the logical host
     */
    String host;

    /**
     * The share name on that host
     */
    String name;

    protected NetworkShare() {
    }

    public NetworkShare(String host, String share) {
        this(null, host, share);
        this.access = new ArrayList<>();
    }

    public NetworkShare(Long id, String host, String name) {
        this.host = host;
        this.name = name;
    }

    public static Path uriToPath(String path) {
        try {
            if (path.startsWith("/")) {
                return Paths.get(path);
            }
            return Paths.get(new URI(path));
        } catch (URISyntaxException e) {
            throw new AssertionError("Unexpected conversion error", e);
        }
    }

    public Collection<NetworkShareAccess> getAccess() {
        if (access == null) {
            try(PreparedStatement st = Scheduler.prepareStatement("SELECT connector, path, priority FROM NetworkShareAccess WHERE share=?")) {
                ArrayList<NetworkShareAccess> accesses = new ArrayList<>();
                st.setLong(1, getId());
                st.execute();
                ResultSet rs = st.getResultSet();
                while (rs.next()) {
                    NetworkShareAccess nsa = new NetworkShareAccess((SingleHostConnector) Connector.findById(rs.getLong(1)), rs.getString(2), rs.getInt(3));
                    nsa.setShare(this);
                    accesses.add(nsa);
                }
                this.access = accesses;
            } catch (SQLException e) {
                throw new XPMRuntimeException(e, "Cannot get share accesses");
            }
        }
        return access;
    }

    public void add(NetworkShareAccess networkShareAccess) throws SQLException {
        // Save access
        networkShareAccess.save(this);

        // Add to ourselves
        access.add(networkShareAccess);
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


    public String getHost() {
        return host;
    }

    public String getName() {
        return name;
    }

    public static final String SELECT_QUERY = "SELECT id, hostname, name FROM NetworkShares";

    public static final String FINDBYNAME_QUERY = SELECT_QUERY + " WHERE hostname=? and name=?";

    static public NetworkShare create(DatabaseObjects<NetworkShare> db, ResultSet result) {
        try {
            long id = result.getLong(1);
            String hostname = null;
            hostname = result.getString(2);


            String name = result.getString(3);

            final NetworkShare networkShare = new NetworkShare(id, hostname, name);
            return networkShare;
        } catch (SQLException e) {
            throw new XPMRuntimeException(e, "Could not construct network share");
        }
    }

    /**
     * Find a connector given its string ID
     *
     * @param host The host name
     * @param name The share name
     * @return The connector in database or null if none exist
     */
    static public NetworkShare find(String host, String name) throws SQLException {
        DatabaseObjects<NetworkShare> shares = Scheduler.get().networkShares();
        return shares.findUnique(FINDBYNAME_QUERY, st -> {
            st.setString(1, host);
            st.setString(2, name);
        });
    }

    public void save() throws SQLException {
        DatabaseObjects<NetworkShare> shares = Scheduler.get().networkShares();
        shares.save(this, "INSERT INTO NetworkShares(hostname, name) VALUES(?, ?)", st -> {
            st.setString(1, getHost());
            st.setString(2, getName());
        });
    }
}
