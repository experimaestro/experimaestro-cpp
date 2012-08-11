package sf.net.experimaestro.exceptions;

/**
 * Exception thrown when a process fails to start
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class LaunchException extends ExperimaestroException {
    public LaunchException() {
        super();
    }

    public LaunchException(String message, Throwable t) {
        super(message, t);
    }

    public LaunchException(Throwable t, String format, Object... values) {
        super(t, format, values);
    }

    public LaunchException(String message) {
        super(message);
    }

    public LaunchException(String format, Object... values) {
        super(format, values);
    }

    public LaunchException(Throwable t) {
        super(t);
    }
}
