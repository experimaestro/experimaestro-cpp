package net.bpiwowar.xpm.commands;

import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.utils.JsonAbstract;
import net.bpiwowar.xpm.utils.PathUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

/**
 * Path content, to be converted in function of the context
 */
@Exposed
public class PathContent implements Content {
    private Path path;

    public PathContent() {}

    public PathContent(Path path) {
        this.path = path;
    }

    @Override
    public void write(CommandContext environment, PrintStream out) throws IOException {
        String resolvedPath = environment.resolve(path, null);
        resolvedPath = PathUtils.protect(resolvedPath, "\"\\");
        out.print(resolvedPath);
    }
}
