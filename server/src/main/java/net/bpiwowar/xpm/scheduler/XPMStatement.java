package net.bpiwowar.xpm.scheduler;

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

import com.google.common.collect.AbstractIterator;
import net.bpiwowar.xpm.exceptions.WrappedException;
import net.bpiwowar.xpm.exceptions.WrappedSQLException;
import net.bpiwowar.xpm.utils.ExceptionalRunnable;
import net.bpiwowar.xpm.utils.StreamUtils;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.stream.Stream;

/**
 * Utility class to execute statements
 */
public class XPMStatement implements AutoCloseable {
    private final PreparedStatement st;

    public XPMStatement(Connection connection, String sql) throws SQLException {
        try {
            st = connection.prepareStatement(sql);
        } catch (SQLException | RuntimeException e) {
            connection.close();
            throw e;
        }
    }

    public PreparedStatement get() {
        return st;
    }

    public XPMStatement setLong(int index, long id) throws SQLException {
        return protect(() -> st.setLong(index, id));
    }

    public XPMStatement setLong(int index, Long value) throws SQLException {
        return protect(() -> {
            if (value == null) {
                st.setNull(index, Types.BIGINT);
            } else {
                st.setLong(index, value);
            }
        });
    }

    private XPMStatement protect(ExceptionalRunnable r) throws SQLException {
        try {
            r.apply();
        } catch (SQLException e) {
            throw e;
        } catch (RuntimeException e) {
            st.close();
            throw new SQLException(e);
        } catch (Throwable e) {
            throw new AssertionError("Should not happen", e);
        }
        return this;
    }

    public XPMStatement execute() throws SQLException {
        protect(() -> st.execute());
        return this;
    }

    public XPMStatement setDouble(int index, double value) throws SQLException {
        return protect(() -> st.setDouble(index, value));
    }

    public XPMStatement setObject(int index, Object object) throws SQLException {
        return protect(() -> st.setObject(index, object));
    }

    public XPMStatement setBlob(int index, InputStream is) throws SQLException {
        return protect(() -> st.setBlob(index, is));
    }

    public XPMStatement setInt(int index, int value) throws SQLException {
        return protect(() -> st.setDouble(index, value));
    }

    public int executeUpdate() throws SQLException {
        try {
            return st.executeUpdate();
        } finally {
            st.close();
        }
    }

    public XPMResultSet resultSet() throws SQLException {
        return new XPMResultSet(st.getResultSet());
    }

    public void close() throws SQLException {
        st.close();
    }

    /**
     * Execute the query and returns the result set
     *
     * @return
     * @throws SQLException
     */
    public XPMResultSet singleResultSet() throws SQLException {
        return singleResultSet(false);
    }


    public XPMResultSet singleResultSet(boolean allowEmpty) throws SQLException {
        st.execute();
        final ResultSet rs = st.getResultSet();
        if (!rs.next()) if (allowEmpty) {
            st.close();
            return null;
        } else
            throw new SQLException("Expected one result only (got 0)");
        if (!rs.isLast()) throw new SQLException("Expected one result only (got > 1)");
        return new XPMResultSet(rs);
    }

    public Stream<XPMResultSet> stream() throws WrappedSQLException {
        return stream(true);
    }

    /**
     * @param close True if the statement should be closed at the end (the result set is always closed)
     * @return
     * @throws WrappedSQLException
     */
    public Stream<XPMResultSet> stream(final boolean close) throws WrappedSQLException {

        try {
            st.execute();
            final XPMResultSet set = resultSet();
            ResultSetCloser closer = new ResultSetCloser(close, set);

            return StreamUtils.stream(new AbstractIterator<XPMResultSet>() {
                @Override
                protected XPMResultSet computeNext() {
                    try {
                        if (set.next()) return set;
                        closer.call();
                        return endOfData();
                    } catch (SQLException e) {
                        throw new WrappedException(e);
                    }
                }
            }).onClose(() -> closer.call());

        } catch (SQLException e) {
            throw new WrappedSQLException(e);
        }

    }

    public XPMStatement setString(int index, String value) throws SQLException {
        st.setString(index, value);
        return this;
    }

    public XPMStatement setTimestamp(int index, Timestamp value) throws SQLException {
        st.setTimestamp(index, value);
        return this;
    }


    private class ResultSetCloser {
        private final boolean close;
        private final XPMResultSet set;

        public ResultSetCloser(boolean close, XPMResultSet set) {
            this.close = close;
            this.set = set;
        }

        public void call() {
            try {
                if (close) {
                    if (!set.isClosed()) {
                        set.close();
                    }
                    if (!st.isClosed()) {
                        st.close();
                    }
                } else {
                    if (!set.resultSet.isClosed()) {
                        set.resultSet.close();
                    }
                }
            } catch (SQLException e) {
                throw new WrappedSQLException(e);
            }
        }
    }
}