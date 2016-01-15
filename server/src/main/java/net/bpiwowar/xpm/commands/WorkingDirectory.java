package net.bpiwowar.xpm.commands;

import java.io.IOException;

/**
 *
 */
public class WorkingDirectory implements CommandComponent {
    static final public WorkingDirectory INSTANCE = new WorkingDirectory();

    private WorkingDirectory() {
    }

    @Override
    public String toString(CommandContext environment) throws IOException {
        return environment.getWorkingDirectory();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WorkingDirectory;
    }
}
