package sf.net.experimaestro.manager;

import sf.net.experimaestro.exceptions.NoSuchParameter;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonArray;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 23/1/13
 */
public class ArrayValue extends Value {
    /** The input nodes */
    JsonArray array;

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
    public void set(Json value) {
        this.array = (JsonArray)value;
    }

    @Override
    public void process(boolean simulate) {
    }

    @Override
    public Json get() {
        return array;
    }
}
