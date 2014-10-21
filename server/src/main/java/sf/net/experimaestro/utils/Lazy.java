package sf.net.experimaestro.utils;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 17/1/13
 */
public class Lazy {
    private final String format;
    private final Object[] objects;

    public Lazy(String format, Object... objects) {
        this.format = format;
        this.objects = objects;
    }

    public static Lazy format(String format, Object... objects) {
        return new Lazy(format, objects);
    }

    @Override
    public String toString() {
        return String.format(format, objects);
    }
}
