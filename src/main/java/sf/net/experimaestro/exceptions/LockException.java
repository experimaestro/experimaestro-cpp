package sf.net.experimaestro.exceptions;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 23/1/13
 */
public class LockException extends ExperimaestroException {
    public LockException() {
    }

    public LockException(String message, Throwable t) {
        super(message, t);
    }

    public LockException(Throwable t, String format, Object... values) {
        super(t, format, values);
    }

    public LockException(String message) {
        super(message);
    }

    public LockException(String format, Object... values) {
        super(format, values);
    }

    public LockException(Throwable t) {
        super(t);
    }
}
