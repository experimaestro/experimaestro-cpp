package net.bpiwowar.xpm.commands;

import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.Dependency;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A sub-command whose output / input can be globally set
 */
@Exposed
public class SubCommand implements CommandComponent {
    /**
     * The command
     */
    Commands commands;

    // Just for serialization
    private SubCommand() {
    }

    SubCommand(Commands commands) {
        this.commands = commands;
    }

    Commands getCommands() {
        return commands;
    }

    @Override
    public Stream<? extends CommandComponent> allComponents() {
        return commands.commands.parallelStream().flatMap(AbstractCommand::allComponents);
    }

    @Override
    public Stream<Dependency> dependencies() {
        return commands.dependencies();
    }

    public Commands get() {
        return commands;
    }

    public Stream<AbstractCommand> commands() {
        return StreamSupport.stream(commands.spliterator(), false);
    }

    @Override
    public void prepare(CommandContext environment) {
        commands.prepare(environment);
    }
}
