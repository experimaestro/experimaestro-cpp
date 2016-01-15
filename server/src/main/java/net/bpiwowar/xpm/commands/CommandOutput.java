package net.bpiwowar.xpm.commands;

import net.bpiwowar.xpm.manager.scripting.Exposed;

import java.io.IOException;
import java.io.Serializable;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Used when the argument should be replaced by a pipe
 */
@Exposed
public class CommandOutput implements CommandComponent, Serializable {
    /**
     * The output
     */
    AbstractCommand command;

    protected CommandOutput() {
    }

    public CommandOutput(AbstractCommand command) {
        this.command = command;
    }


    @Override
    public void prepare(CommandContext environment) throws IOException {
        final java.nio.file.Path file = environment.getUniqueFile("command", ".pipe");
        final Object o = environment.setData(this, file);
        if (o != null) throw new RuntimeException("CommandOutput data should be null");
        environment.getNamedRedirections(command, true).outputRedirections.add(file);
        environment.detached(command, true);
    }

    @Override
    public String toString(CommandContext environment) throws IOException {
        final Object data = environment.getData(this);
        return environment.resolve((java.nio.file.Path) data);
    }

    @Override
    public Stream<? extends AbstractCommand> commands() {
        return Stream.of(command);
    }

    public AbstractCommand getCommand() {
        return command;
    }
}
