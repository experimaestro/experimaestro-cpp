package net.bpiwowar.xpm.commands;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.utils.UUIDObject;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Used when the argument should be replaced by a pipe
 */
@Exposed
public class CommandOutput extends CommandComponent implements  Serializable {
    /**
     * The output
     */
    transient AbstractCommand command;



    protected CommandOutput() {
    }

    public CommandOutput(AbstractCommand command) {
        this.command = command;
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
    public void postJSONSave(JsonWriter out) throws IOException {
        super.postJSONSave(out);
        out.name("command");
        out.value(command.getUUID());
    }

    @Override
    public void postJSONLoad(Map<String, UUIDObject> map, JsonReader in, String name) throws IOException {
        switch (name) {
            case "command":
                final String key = in.nextString();
                command = (AbstractCommand) map.get(key);
                assert command != null;
                break;
            default:
                super.postJSONLoad(map, in, name);
        }
    }
}
