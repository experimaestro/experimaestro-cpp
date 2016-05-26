package net.bpiwowar.xpm.exceptions;

/**
 * Exception related to a connector
 */
public class ConnectorException extends ContextualThrowable {
    public ConnectorException() {
        super();
    }

    public ConnectorException(String message, Throwable t) {
        super(message, t);
    }

    public ConnectorException(Throwable t, String format, Object... values) {
        super(t, format, values);
    }

    public ConnectorException(String message) {
        super(message);
    }

    public ConnectorException(String format, Object... values) {
        super(format, values);
    }

    public ConnectorException(Throwable t) {
        super(t);
    }
}
