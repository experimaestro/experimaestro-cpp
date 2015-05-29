package sf.net.experimaestro.scheduler;

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

import sf.net.experimaestro.connectors.NetworkShare;
import sf.net.experimaestro.exceptions.DatabaseException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Access to connectors
 */
public class NetworkShares extends DatabaseObjects<NetworkShare> {
    public static final String SELECT_QUERY = "SELECT id, hostname, name FROM NetworkShares";

    public static final String FINDBYNAME_QUERY = SELECT_QUERY + " WHERE host=? and name=?";

    public NetworkShares(Connection connection) {
        super(connection);
    }

    @Override
    protected NetworkShare create(ResultSet result) throws DatabaseException {
        try {
            long id = result.getLong(1);
            String hostname = result.getString(2);
            String name = result.getString(3);

            final NetworkShare networkShare = new NetworkShare(hostname, name);
            networkShare.setId(id);
            return networkShare;

        } catch (SQLException e) {
            throw new DatabaseException(e, "Error retrieving database object");
        }
    }

    public NetworkShare find(String host, String name) throws DatabaseException {
        return findUnique(FINDBYNAME_QUERY, st -> {
            st.setString(1, host);
            st.setString(2, name);
        });
    }
}
