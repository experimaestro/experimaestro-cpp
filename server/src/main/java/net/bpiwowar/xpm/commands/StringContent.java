package net.bpiwowar.xpm.commands;

import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.utils.JsonAbstract;

import java.io.PrintStream;

/**
 * String content
 */
@Exposed
public class StringContent implements Content {
    private final String content;

    @Expose
    public StringContent(String content) {
        this.content = content;
    }

    @Override
    public void write(CommandContext environment, PrintStream out) {
        out.print(content);
    }
}
