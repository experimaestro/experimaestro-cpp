package sf.net.experimaestro.exceptions;

/**
 * Exception that are caused by the execution of a script or a command - they are
 * not errors
 */
public class XPMCommandException extends XPMRuntimeException {
    public XPMCommandException() {
    }

    public XPMCommandException(String message, Throwable t) {
        super(message, t);
    }

    public XPMCommandException(Throwable t, String format, Object... values) {
        super(t, format, values);
    }

    public XPMCommandException(String message) {
        super(message);
    }

    public XPMCommandException(String format, Object... values) {
        super(format, values);
    }

    public XPMCommandException(Throwable t) {
        super(t);
    }
}
