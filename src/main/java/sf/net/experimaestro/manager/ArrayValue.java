package sf.net.experimaestro.manager;

import org.w3c.dom.Document;

import java.util.ArrayList;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 23/1/13
 */
public class ArrayValue extends Value {
    ArrayList<Document> documents;

    public ArrayValue() {
    }

    @Override
    public void set(DotName id, Document value) {

    }

    @Override
    public void process() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Document get() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
