package net.bpiwowar.xpm.commands;

import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.Dependency;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A sub-command whose output / input can be globally set
 */
@Exposed
public class SubCommand implements CommandComponent {
    /**
     * The commands
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

    public void forEachDependency(Consumer<Dependency> consumer) {
        commands.forEachDependency(consumer);
    }

    @Override
    public void forEachCommand(Consumer<? super AbstractCommand> consumer) {
        for (AbstractCommand command : commands) {
            consumer.accept(command);
            command.forEachCommand(consumer);
        }
    }

    public Commands commands() {
        return commands;
    }

    @Override
    public void prepare(CommandContext environment) {
        commands.prepare(environment);
    }
}
