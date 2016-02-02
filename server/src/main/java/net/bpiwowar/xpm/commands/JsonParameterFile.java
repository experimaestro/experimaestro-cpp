package net.bpiwowar.xpm.commands;

import com.google.common.collect.ImmutableSet;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.json.JsonWriterMode;
import net.bpiwowar.xpm.manager.json.JsonWriterOptions;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;

/**
 * A Json parameter file
 */
@Exposed
public class JsonParameterFile extends CommandComponent {
    private String key;

    private Json json;

    private JsonParameterFile() {
    }

    @Expose
    public JsonParameterFile(String key, Json json) {
        this.key = key;
        this.json = json;
    }

    @Override
    public void prepare(CommandContext environment) throws IOException {
        if (environment.getData(this) != null) {
            return;
        }
        environment.setData(this, environment.getAuxiliaryFile(key, ".json"));
    }

    @Override
    public String toString(CommandContext environment) throws IOException {
        java.nio.file.Path file = (java.nio.file.Path) environment.getData(this);
        try (OutputStream out = Files.newOutputStream(file);
             OutputStreamWriter jsonWriter = new OutputStreamWriter(out)) {
            final JsonWriterOptions options = new JsonWriterOptions(ImmutableSet.of())
                    .ignore$(false)
                    .ignoreNull(false)
                    .mode(JsonWriterMode.PARAMETER_FILE)
                    .simplifyValues(true)
                    .removeDefault(false)
                    .resolveFile(f -> {
                        try {
                            return environment.resolve(f, null);
                        } catch (IOException e) {
                            throw new XPMRuntimeException(e);
                        }
                    });
            json.writeDescriptorString(jsonWriter, options);
        } catch (IOException e) {
            throw new XPMRuntimeException(e, "Could not write JSON string for java task");
        }

        return environment.resolve(file, environment.getWorkingDirectory());
    }
}
