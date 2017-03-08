package net.bpiwowar.xpm.commands;

import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.utils.JsonAbstract;

import java.io.IOException;
import java.io.PrintStream;

/**
 * An abstract content
 */
@Exposed
@JsonAbstract
public interface Content {
    void write(CommandContext environment, PrintStream out) throws IOException;
}
