package net.bpiwowar.xpm.manager.tasks;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import net.bpiwowar.xpm.commands.JsonParameterFile;
import net.bpiwowar.xpm.exceptions.XPMAssertionError;
import net.bpiwowar.xpm.commands.Command;

import java.lang.reflect.Type;
import java.nio.file.Path;

/**
 * Represents a command line argument
 */
public interface CommandArgument {
    void process(ScriptCommandBuilder builder, Command command, Path scriptPath, JsonParameterFile jsonParameter);

    class CommandString implements CommandArgument {
        String string;

        public CommandString(String string) {
            this.string = string;
        }

        @Override
        public void process(ScriptCommandBuilder builder, Command command, Path scriptPath, JsonParameterFile jsonParameter) {
            command.add(string);
        }
    }

    enum Variables {
        script, json
    }

    class CommandVariable implements CommandArgument {
        Variables variable;

        public CommandVariable(String name) {
            this.variable = Variables.valueOf(name);
        }

        @Override
        public void process(ScriptCommandBuilder builder, Command command, Path scriptPath, JsonParameterFile jsonParameter) {
            switch (variable) {
                case script:
                    command.add(scriptPath);
                    break;
                case json:
                    command.add(jsonParameter);
                    break;
                default:
                    throw new XPMAssertionError("Cannot handle variable %s", variable);
            }
        }

    }

    class TypeAdapter implements JsonDeserializer {

        public static final String VARIABLE = "variable";

        @Override
        public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json instanceof JsonPrimitive)
                return new CommandString(json.getAsString());

            if (json instanceof JsonObject) {
                final JsonObject jsonObject = (JsonObject) json;
                if (jsonObject.has("variable")) {
                    final JsonPrimitive name = jsonObject.getAsJsonPrimitive(VARIABLE);
                    return new CommandVariable(name.getAsString());
                }
            }

            throw new XPMAssertionError("Cannot deserialize %s as command argument", json);
        }
    }
}