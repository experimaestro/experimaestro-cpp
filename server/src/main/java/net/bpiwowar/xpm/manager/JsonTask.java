package net.bpiwowar.xpm.manager;

import com.google.gson.stream.JsonWriter;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.manager.json.JsonString;
import net.bpiwowar.xpm.manager.json.JsonWriterMode;
import net.bpiwowar.xpm.manager.json.JsonWriterOptions;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.ExposeMode;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.manager.scripting.Property;
import net.bpiwowar.xpm.scheduler.Commands;
import net.bpiwowar.xpm.scheduler.Dependency;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Something that can be run similarly to factories
 */
@Exposed
public class JsonTask extends Json {
    /** The task */
    private final JsonObject json;

    /** The commands */
    private final Commands commands;

    public JsonTask(Task json, Commands commands) {
        this.json = json.getOutputJson();
        this.commands = commands;
    }

    @Expose(mode = ExposeMode.PROPERTY, value = "json")
    public JsonObject getJson() {
        return json;
    }

    @Expose(mode = ExposeMode.PROPERTY, value = "commands")
    public Commands getCommands() {
        return commands;
    }

    @Override
    public Object get() {
        return commands;
    }

    @Override
    public QName type() {
        return Constants.XP_INPUT_STREAM;
    }

    @Override
    public void write(Writer out) throws IOException {
        json.write(out);
    }

    @Override
    public void write(JsonWriter out) throws IOException {
        json.write(out);
    }

    @Override
    public void writeDescriptorString(Writer writer, JsonWriterOptions options) throws IOException {
        if (options.mode != JsonWriterMode.PARAMETER_FILE) {
            json.writeDescriptorString(writer, options);
        } else {
            // Write the command instead
            new JsonString("hello").write(writer);
        }
    }

    public Stream<Dependency> dependencies() {
        return commands.dependencies();
    }


}
