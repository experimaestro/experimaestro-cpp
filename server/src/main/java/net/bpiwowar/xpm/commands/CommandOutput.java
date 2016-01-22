package net.bpiwowar.xpm.commands;

import com.google.gson.annotations.JsonAdapter;
import net.bpiwowar.xpm.manager.scripting.Exposed;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.stream.Stream;

/**
 * Used when the argument should be replaced by a pipe
 */
@Exposed
public class CommandOutput implements CommandComponent, Serializable {
    /**
     * The output
     */
    transient AbstractCommand command;

    /**
     * command UUID
     */
    String commandUUID;


    protected CommandOutput() {
    }

    public CommandOutput(AbstractCommand command) {
        this.command = command;
        this.commandUUID = command.getUUID();
    }


    @Override
    public void prepare(CommandContext environment) throws IOException {
        command.prepare(environment);
        final java.nio.file.Path file = environment.getUniqueFile("command", ".pipe");
        final Object o = environment.setData(this, file);
        if (o != null) throw new RuntimeException("CommandOutput data should be null");
        environment.getNamedRedirections(command, true).outputRedirections.add(file);
        environment.detached(command, true);
    }

    @Override
    public String toString(CommandContext environment) throws IOException {
        final Path path = getFile(environment);
        return environment.resolve(path, null);
    }

    public Path getFile(CommandContext environment) {
        return (Path) environment.getData(this);
    }

    @Override
    public Stream<? extends AbstractCommand> commands() {
        return Stream.of(command);
    }

    public AbstractCommand getCommand() {
        return command;
    }

    @Override
    public void init(HashMap<String, Object> uuidMap) {
        command = (AbstractCommand) uuidMap.get(commandUUID);
    }
}
