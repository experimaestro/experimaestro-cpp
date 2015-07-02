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

import sf.net.experimaestro.utils.ExceptionalRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Utility class to execute statements
 */
public class XPMStatement {
    private final PreparedStatement st;

    public XPMStatement(Connection connection, String sql) throws SQLException {
        try {
            st = connection.prepareStatement(sql);
        } catch (SQLException | RuntimeException e) {
            connection.close();
            throw e;
        }
    }

    public XPMStatement setLong(int index, long id) throws SQLException {
        return protect(() -> st.setLong(index, id));
    }

    public XPMStatement setLong(int index, Long value) throws SQLException {
        return protect(() -> {
            if (value == null) {
                st.setLong(index, value);
            } else {
                st.setNull(index, Types.BIGINT);
            }
        });
    }

    private XPMStatement protect(ExceptionalRunnable r) throws SQLException {
        try {
            r.apply();
        } catch (RuntimeException | SQLException e) {
            st.close();
        } catch (Throwable e) {
            throw new AssertionError("Should not happen", e);
        }
        return this;
    }

    public void execute() throws SQLException {
        protect(() -> st.execute());
        st.close();
    }

    public XPMStatement setDouble(int index, double value) throws SQLException {
        return protect(() -> st.setDouble(index, value));
    }

    public XPMStatement setObject(int index, Object object) throws SQLException {
        return protect(() -> st.setObject(index, object));
    }

    public XPMStatement setInt(int index, int value) throws SQLException {
        return protect(() -> st.setDouble(index, value));
    }
}
