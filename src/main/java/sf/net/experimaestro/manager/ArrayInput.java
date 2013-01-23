package sf.net.experimaestro.manager;

/**
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 23/1/13
 */
public class ArrayInput extends Input {
    private static final Type TYPE = new Type(new QName(Manager.EXPERIMAESTRO_NS, "array"));

    public ArrayInput() {
        super(TYPE);
    }

    @Override
    Value newValue() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
