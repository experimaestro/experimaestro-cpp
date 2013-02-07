package sf.net.experimaestro.manager;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.NoSuchParameter;

import java.util.ArrayList;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 23/1/13
 */
public class ArrayValue extends Value {
    ArrayList<Node> documents;

    public ArrayValue() {
    }

    @Override
    public Value getValue(DotName id) throws NoSuchParameter {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void set(Node value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void process() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public DocumentFragment get() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
