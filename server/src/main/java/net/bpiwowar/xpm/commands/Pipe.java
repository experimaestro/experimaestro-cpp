package net.bpiwowar.xpm.commands;

import net.bpiwowar.xpm.manager.scripting.Exposed;

/**
 * A pipe
 */
@Exposed
public class Pipe extends CommandComponent {
    static private Pipe PIPE = new Pipe();

    private Pipe() {
    }

    public static Pipe getInstance() {
        return PIPE;
    }
}
