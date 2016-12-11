package net.bpiwowar.xpm.commands;

import com.google.gson.annotations.JsonAdapter;
import net.bpiwowar.xpm.manager.scripting.Argument;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.JsonPathConverter;
import net.bpiwowar.xpm.utils.PathUtils;

import java.io.IOException;
import java.io.Serializable;

/**
 *
 */
@Exposed
public class CommandPath extends CommandComponent implements Serializable {
    @JsonAdapter(JsonPathConverter.class)
    private java.nio.file.Path file;

    private CommandPath() {
    }

    @Expose
    public CommandPath(@Argument(name = "pathname") String filepath) throws IOException {
        file = PathUtils.toPath(filepath);
    }

    public CommandPath(java.nio.file.Path file) {
        this.file = file;
    }

    @Override
    public String toString(CommandContext environment) throws IOException {
        return environment.resolve(file, null);
    }

    @Override
    public String toString() {
        return String.format("Path{%s}", file.toUri());
    }
}
