package net.bpiwowar.xpm.exceptions;

/**
 *
 */
public class XPMScriptRuntimeException extends XPMRuntimeException {
    public XPMScriptRuntimeException() {
    }

    public XPMScriptRuntimeException(String message, Throwable t) {
        super(message, t);
    }

    public XPMScriptRuntimeException(Throwable t, String format, Object... values) {
        super(t, format, values);
    }

    public XPMScriptRuntimeException(String message) {
        super(message);
    }

    public XPMScriptRuntimeException(String format, Object... values) {
        super(format, values);
    }

    public XPMScriptRuntimeException(Throwable t) {
        super(t);
    }
}
