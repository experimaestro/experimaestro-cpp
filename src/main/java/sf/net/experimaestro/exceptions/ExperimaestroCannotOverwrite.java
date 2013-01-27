package sf.net.experimaestro.exceptions;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 27/1/13
 */
public class ExperimaestroCannotOverwrite extends ExperimaestroException {
    public ExperimaestroCannotOverwrite() {
    }

    public ExperimaestroCannotOverwrite(String message, Throwable t) {
        super(message, t);
    }

    public ExperimaestroCannotOverwrite(Throwable t, String format, Object... values) {
        super(t, format, values);
    }

    public ExperimaestroCannotOverwrite(String message) {
        super(message);
    }

    public ExperimaestroCannotOverwrite(String format, Object... values) {
        super(format, values);
    }

    public ExperimaestroCannotOverwrite(Throwable t) {
        super(t);
    }
}
