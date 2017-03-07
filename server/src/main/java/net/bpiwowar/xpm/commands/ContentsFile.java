package net.bpiwowar.xpm.commands;

import net.bpiwowar.xpm.manager.scripting.Argument;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

/**
 *
 */
@Exposed
public class ContentsFile extends CommandComponent implements Serializable {
    /** File name */
    String key;

    /** File name extension */
    String extension;

    /** Contents */
    ArrayList<Content> contents = new ArrayList<>();

    private ContentsFile() {
    }

    @Expose
    public ContentsFile(@Argument(name = "key") String key, @Argument(name = "extension") String extension) {
        this.key = key;
        this.extension = extension;
    }

    @Expose
    public void add(Content content) {
        contents.add(content);
    }

    @Expose
    public void add(String content) {
        contents.add(new StringContent(content));
    }

    @Expose
    public void add(Path path) {
        contents.add(new PathContent(path));
    }

    @Override
    public String toString(CommandContext environment) throws IOException {
        java.nio.file.Path file = environment.getAuxiliaryFile(key, extension);

        try(OutputStream out = Files.newOutputStream(file); PrintStream ps = new PrintStream(out)) {
            for (Content content : contents) {
                content.write(environment, ps);
            }
        }

        return environment.resolve(file, null);
    }

    @Override
    public String toString() {
        return String.format("ContentFile(%s,%s)", key, extension);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentsFile that = (ContentsFile) o;
        return Objects.equals(key, that.key) &&
                Objects.equals(extension, that.extension) &&
                Objects.equals(contents, that.contents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, extension, contents);
    }
}
