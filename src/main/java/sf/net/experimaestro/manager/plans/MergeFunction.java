package sf.net.experimaestro.manager.plans;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonArray;
import sf.net.experimaestro.manager.json.JsonObject;

import java.util.Map;

/**
 * A function that simply merge JSON objects
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 26/4/13
 */
public class MergeFunction implements Function {
    private final QName outputType;
    private final Int2ObjectOpenHashMap<String> map;

    public MergeFunction(QName outputType, Int2ObjectOpenHashMap<String> map) {
        this.outputType = outputType;
        this.map = map;
    }

    @Override
    public JsonArray f(Json[] input) {
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
                            throw new ExperimaestroRuntimeException("Conflicting id in merge: %s", key);
                        returned.put(key, entry.getValue());
                    }
                } else {
                    ExperimaestroRuntimeException e = new ExperimaestroRuntimeException("Can only merge JSON objects, but not %s", json.getClass());
                    if (outputType != null)
                        e.addContext("while merging into JSON object of type %s", outputType);
                    throw e;
                }
            }
        }
        if (outputType != null)
            returned.put(Manager.XP_TYPE.toString(), outputType.toString());
        JsonArray array = new JsonArray();
        array.add(returned);
        return array;
    }

    @Override
    public String toString() {
        return "merge";
    }
}
