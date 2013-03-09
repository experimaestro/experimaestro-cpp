package sf.net.experimaestro.manager;

import org.w3c.dom.Document;
import sf.net.experimaestro.exceptions.NoSuchParameter;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 23/1/13
 */
public class ArrayValue extends Value {
    /** The input nodes */
    Document array;

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
    public void set(Document value) {
        this.array = value;
//        if (value instanceof DocumentFragment) {
//            array = value;
//
//        }
//
//        else if (value instanceof Document) {
//            array = ((Document) value).createDocumentFragment();
//            array.appendChild(((Document) value).getDocumentElement());
//        }
//
//        else
//            throw new IllegalArgumentException(String.format("Cannot handle type %s", value.getClass()));
    }

    @Override
    public void process(boolean simulate) {
    }

    @Override
    public Document get() {
        return array;
    }
}
