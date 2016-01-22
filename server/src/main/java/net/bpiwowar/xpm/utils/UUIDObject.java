package net.bpiwowar.xpm.utils;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Map;

/**
 * Interface for unique identifiable objects
 */
@JsonAbstract
public interface UUIDObject {
    String getUUID();

    void postJSONSave(JsonWriter out) throws IOException;

    void postJSONLoad(Map<String, UUIDObject> map, JsonReader in, String name) throws IOException;

    void setUUID(String uuid);
}
