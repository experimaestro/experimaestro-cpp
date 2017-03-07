package net.bpiwowar.xpm.commands;

import net.bpiwowar.xpm.manager.scripting.Exposed;

import java.io.IOException;
import java.io.PrintStream;

/**
 * An abstract content
 */
@Exposed
public interface Content {
    void write(CommandContext environment, PrintStream out) throws IOException;
}
