package net.bpiwowar.xpm.commands;

import java.io.IOException;

/**
 *
 */
public class WorkingDirectory extends CommandComponent {
    static final public WorkingDirectory INSTANCE = new WorkingDirectory();

    private WorkingDirectory() {
    }

    @Override
    public String toString(CommandContext environment) throws IOException {
        return environment.resolve(environment.getWorkingDirectory(), null);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WorkingDirectory;
    }
}
