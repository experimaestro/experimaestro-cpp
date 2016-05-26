package net.bpiwowar.xpm.exceptions;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * Throwable with context
 */
public class ContextualThrowable extends Exception implements ContextualException {
    ArrayList<String> context = new ArrayList<>();

    public ContextualThrowable() {
        super();
    }

    public ContextualThrowable(String message, Throwable t) {
        super(message, t);
    }

    public ContextualThrowable(Throwable t, String format, Object... values) {
        super(String.format(format, values), t);
    }

    public ContextualThrowable(String message) {
        super(message);
    }

    public ContextualThrowable(String format, Object... values) {
        super(String.format(format, values));
    }

    public ContextualThrowable(Throwable t) {
        super(t);
    }

    public ContextualThrowable addContext(String format, Object... values) {
        context.add(format(format, values));
        return this;
    }

    public List<String> getContext() {
        return context;
    }
}
