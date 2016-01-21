package net.bpiwowar.xpm.exceptions;

import java.sql.SQLException;

/**
 * Wrapped SQL exception
 */
public class WrappedSQLException extends WrappedException {
    public WrappedSQLException(SQLException e) {
        super(e);
    }
}
