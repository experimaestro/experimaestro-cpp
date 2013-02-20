package sf.net.experimaestro.manager;

import org.w3c.dom.Document;
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
        if (id.isEmpty())
            return this;

        throw new NoSuchParameter("Unknown parameter name: " + id.toString());
    }

    @Override
    public void set(Node value) {
        if (value instanceof DocumentFragment) {
            fragment = value;

        }

        else if (value instanceof Document) {
            fragment = ((Document) value).createDocumentFragment();
            fragment.appendChild(((Document) value).getDocumentElement());
        }

        else throw new IllegalArgumentException(String.format("Cannot handle type %s", value.getClass()));
    }

    @Override
    public void process() {
    }

    @Override
    public Node get() {
        return fragment;
    }
}
