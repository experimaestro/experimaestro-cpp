package net.bpiwowar.xpm.commands;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.Dependency;
import net.bpiwowar.xpm.utils.UUIDObject;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A sub-command whose output / input can be globally set
 */
@Exposed
public class SubCommand extends CommandComponent {
    /**
     * The command
     */
    transient Commands commands;

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
    public Stream<? extends AbstractCommandComponent> allComponents() {
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

    @Override
    public void postJSONSave(JsonWriter out) throws IOException {
        super.postJSONSave(out);
        out.name("commands");
        out.value(commands.getUUID());
    }

    @Override
    public void postJSONLoad(Map<String, UUIDObject> map, JsonReader in, String name) throws IOException {
        switch (name) {
            case "commands":
                commands = (Commands) map.get(in.nextString());
                break;
            default:
                super.postJSONLoad(map, in, name);
        }
    }
}
