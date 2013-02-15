package sf.net.experimaestro.manager;

/**
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 23/1/13
 */
public class ArrayInput extends Input {
    /** The qualified name for this type */
    private static final Type TYPE = new Type(new QName(Manager.EXPERIMAESTRO_NS, "sequence"));

    /** The type of the array elements */
    private final Input innerType;

    public ArrayInput(Input innerType) {
        super(TYPE);
        this.innerType = innerType;
    }

    @Override
    Value newValue() {
        return new ArrayValue(this);
    }
}
