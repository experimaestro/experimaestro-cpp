package net.bpiwowar.xpm.commands;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.utils.UUIDObject;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 *
 */
@Exposed
public class CommandComponent implements AbstractCommandComponent {
    /**
     * Command UUID
     */
    transient private String uuid = UUID.randomUUID().toString();

    @Override
    public String getUUID() {
        return uuid;
    }

    @Override
    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public void postJSONSave(JsonWriter out) throws IOException {

    }

    @Override
    public void postJSONLoad(Map<String, UUIDObject> map, JsonReader in, String name) throws IOException {

    }
}
