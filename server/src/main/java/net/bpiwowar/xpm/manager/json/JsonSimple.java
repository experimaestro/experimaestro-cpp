package net.bpiwowar.xpm.manager.json;

import net.bpiwowar.xpm.manager.scripting.Exposed;

/**
 * Base class for all simple values
 */
@Exposed
public abstract class JsonSimple extends Json {
    @Override
    final public boolean isSimple() {
        return true;
    }

    @Override
    public Json annotate(String key, Json value) {
        final JsonObject jsonObject = toJsonObject();
        jsonObject.put(key, value);
        return jsonObject;
    }

    public JsonObject toJsonObject() {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.put(JsonObject.XP_VALUE_STRING, this);
        jsonObject.put(JsonObject.XP_TYPE_STRING, type().toString());
        return jsonObject;
    }
}
