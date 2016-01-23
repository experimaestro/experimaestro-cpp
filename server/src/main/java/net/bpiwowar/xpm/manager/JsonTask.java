package net.bpiwowar.xpm.manager;

import com.google.gson.stream.JsonWriter;
import net.bpiwowar.xpm.commands.AbstractCommand;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.manager.json.JsonString;
import net.bpiwowar.xpm.manager.json.JsonWriterMode;
import net.bpiwowar.xpm.manager.json.JsonWriterOptions;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.ExposeMode;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.Dependency;

import java.io.IOException;
import java.io.Writer;
import java.util.stream.Stream;

/**
 * Something that can be run similarly to factories
 */
@Exposed
public class JsonTask extends Json {
    /** The task */
    private final JsonObject json;

    /** The command */
    private final AbstractCommand command;

    public JsonTask(Task json, AbstractCommand command) {
        this.json = json.getOutputJson();
        this.command = command;
    }

    @Expose(mode = ExposeMode.PROPERTY, value = "json")
    public JsonObject getJson() {
        return json;
    }

    @Expose(mode = ExposeMode.PROPERTY, value = "command")
    public AbstractCommand getCommand() {
        return command;
    }

    @Override
    public Object get() {
        return command;
    }

    @Override
    public TypeName type() {
        return Constants.XP_INPUT_STREAM;
    }

    @Override
    public void write(JsonWriter out) throws IOException {
        json.write(out);
    }

    @Override
    public void writeDescriptorString(JsonWriter writer, JsonWriterOptions options) throws IOException {
        if (options.mode != JsonWriterMode.PARAMETER_FILE) {
            json.writeDescriptorString(writer, options);
        } else {
            // Write the command instead
            new JsonString("<standard input>").write(writer);
        }
    }

    public Stream<? extends Dependency> dependencies() {
        return command.dependencies();
    }


}
