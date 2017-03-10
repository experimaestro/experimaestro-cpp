package net.bpiwowar.xpm.commands;

import net.bpiwowar.xpm.manager.scripting.Argument;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 */
@Exposed
public class ParameterFile extends CommandComponent implements Serializable {
    String key;

    byte[] content;

    private ParameterFile() {
    }

    @Expose
    public ParameterFile(String key, byte[] content) {
        this.key = key;
        this.content = content;
    }

    @Expose
    public ParameterFile(@Argument(name="key") String key, @Argument(name="content")String content) {
        this.key = key;
        this.content = content.getBytes();
    }

    @Override
    public String toString(CommandContext environment) throws IOException {
        java.nio.file.Path file = environment.getAuxiliaryFile(key, ".input");
        OutputStream out = Files.newOutputStream(file);
        out.write(content);
        out.close();

        return environment.resolve(file, null);
    }

    @Override
    public String toString() {
        return String.format("ParameterFile(%s)", key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterFile that = (ParameterFile) o;
        return Objects.equals(key, that.key) &&
                Arrays.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, content);
    }
}
