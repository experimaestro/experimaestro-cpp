package sf.net.experimaestro.manager.plans.functions;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonArray;
import sf.net.experimaestro.manager.json.JsonObject;

import java.util.Iterator;
import java.util.Map;

/**
 * A function that simply merge JSON objects
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class MergeFunction implements Function {
    private final QName outputType;
    private final Int2ObjectOpenHashMap<String> map;

    public MergeFunction(QName outputType, Int2ObjectOpenHashMap<String> map) {
        this.outputType = outputType;
        this.map = map;
    }

    @Override
    public Iterator<? extends Json> apply(Json[] input) {
        JsonObject returned = new JsonObject();
        for (int i = 0; i < input.length; i++) {
            Json json = input[i];
            String oKey = map.get(i);
            if (oKey != null) {
                returned.put(oKey, json);
            } else {
                if (json instanceof JsonObject) {
                    for (Map.Entry<String, Json> entry : ((JsonObject) json).entrySet()) {
                        String key = entry.getKey();
                        if (returned.containsKey(key.toString()))
                            throw new XPMRuntimeException("Conflicting id in merge: %s", key);
                        returned.put(key, entry.getValue());
                    }
                } else {
                    XPMRuntimeException e = new XPMRuntimeException("Can only merge JSON objects, but not %s", json.getClass());
                    if (outputType != null)
                        e.addContext("while merging into JSON object of type %s", outputType);
                    throw e;
                }
            }
        }
        if (outputType != null)
            returned.put(Manager.XP_TYPE.toString(), outputType.toString());
        return ImmutableList.of(returned).iterator();
    }

    @Override
    public String toString() {
        return "merge";
    }
}
