package net.bpiwowar.xpm.connectors;

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

import net.bpiwowar.xpm.fs.XPMPath;
import net.bpiwowar.xpm.scheduler.Scheduler;

import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static java.lang.String.format;

/**
 * An access to a network share
 */
final public class NetworkShareAccess implements Serializable {
    /**
     * The host that allows us to access the data
     */
    SingleHostConnector connector;

    NetworkShare share;

    /**
     * Path on the single host connector
     */
    String path;


    /**
     * The priority of this access
     */
    int priority;

    public NetworkShareAccess(SingleHostConnector connector, String path, int priority) {
        this.connector = connector;
        this.path = path;
        this.priority = priority;
    }

    public NetworkShareAccess() {
    }

    public void setShare(NetworkShare share) {
        this.share = share;
    }

    public boolean is(SingleHostConnector connector) {
        return connector.equals(this.connector);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetworkShareAccess that = (NetworkShareAccess) o;

        if (!connector.equals(that.connector)) return false;
        return share.equals(that.share);

    }

    @Override
    public int hashCode() {
        int result = connector.hashCode();
        result = 31 * result + share.hashCode();
        return result;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) throws SQLException {
        this.priority = priority;
        if (share != null) {
            try (PreparedStatement st = Scheduler.getConnection()
                    .prepareStatement("UPDATE NetworkShareAccess SET priority=? WHERE share=? AND connector=?")) {
                st.setInt(1, priority);
                st.setLong(2, share.getId());
                st.setLong(3, connector.getId());
            }
        }
    }

    public SingleHostConnector getConnector() {
        return connector;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) throws SQLException {
        if (share != null) {
            try (PreparedStatement st = Scheduler.getConnection()
                    .prepareStatement("UPDATE NetworkShareAccess SET path=? WHERE share=? AND connector=?")) {
                st.setString(1, path);
                st.setLong(2, share.getId());
                st.setLong(3, connector.getId());
            }
        }
        this.path = path;
    }

    public String resolve(XPMPath path) throws IOException {
        return resolve(path, null);
    }

    public String resolve(XPMPath path, XPMPath reference) throws IOException {
        if (reference != null) {
            return reference.relativize(path).toString();
        }
        return path.getLocalStringPath(getLocalPath(path));
    }

    /**
     * Checks that the path belongs to the share, and returns the path on the host
     * @param path The path to check
     * @return A string representing the path on the host
     */
    protected String getLocalPath(XPMPath path) {
        if (!path.getHostName().equals(share.host) || !path.getShareName().equals(share.name)) {
            throw new IllegalArgumentException(format("Cannot resolve %s for share %s", path, share));
        }
        return this.path;
    }

    /**
     * Save and set a share
     * @param share The shared network volume
     * @throws SQLException If something goes wrong
     */
    public void save(NetworkShare share) throws SQLException {
        // Add to database
        try (PreparedStatement st = Scheduler.getConnection()
                .prepareStatement("INSERT INTO NetworkShareAccess(share, connector, path) VALUES(?,?,?)")) {
            st.setLong(1, share.getId());
            st.setLong(2, connector.getId());
            st.setString(3, getPath());
            st.execute();

            this.share = share;
        }
    }
}
