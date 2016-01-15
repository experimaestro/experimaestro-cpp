package net.bpiwowar.xpm.commands;

import com.google.gson.annotations.JsonAdapter;
import net.bpiwowar.xpm.scheduler.JsonPathConverter;

import java.io.IOException;
import java.io.Serializable;

/**
 *
 */
public class CommandPath implements CommandComponent, Serializable {
    @JsonAdapter(JsonPathConverter.class)
    private java.nio.file.Path file;

    private CommandPath() {
    }

    public CommandPath(java.nio.file.Path file) {
        this.file = file;
    }

    @Override
    public String toString(CommandContext environment) throws IOException {
        return environment.resolve(file);
    }

    @Override
    public String toString() {
        return String.format("Path{%s}", file.toUri());
    }
}
