package sf.net.experimaestro.manager;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.NoSuchParameter;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 23/1/13
 */
public class ArrayValue extends Value {
    /** The input nodes */
    Node fragment;

    public ArrayValue(ArrayInput input) {
        this.input = input;
    }

    @Override
    public Value getValue(DotName id) throws NoSuchParameter {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void set(Node value) {
        if (value instanceof DocumentFragment) {
            fragment = value;
        }
        throw new IllegalArgumentException(String.format("Cannot handle type %s", value.getClass()));
    }

    @Override
    public void process() {
    }

    @Override
    public Node get() {
        return fragment;
    }
}
