package sf.net.experimaestro.manager;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 23/1/13
 */
public class ArrayInput extends Input {
    public ArrayInput(Type innerType) {
        super(new ArrayType(innerType));
    }

    @Override
    Value newValue() {
        return new ArrayValue(this);
    }
}
