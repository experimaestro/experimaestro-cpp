package sf.net.experimaestro.scheduler;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.annotations.JsonAdapter;
import org.mozilla.javascript.NativeArray;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.js.JSParameterFile;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonPath;
import sf.net.experimaestro.manager.json.JsonWriterOptions;
import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.manager.scripting.ScriptingPath;
import sf.net.experimaestro.utils.Functional;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A command line argument (or argument part)
 *
 * @author B. Piwowarski
 */
@Exposed
public class Command extends AbstractCommand implements CommandComponent, Serializable {
    public final static Logger LOGGER = Logger.getLogger();

    /**
     * The list of components in this command
     */
    ArrayList<CommandComponent> list;

    @Expose
    public Command() {
        list = new ArrayList<>();
    }

    public Command(CommandComponent... c) {
        list = new ArrayList<>(Arrays.asList(c));
    }
    public Command(java.lang.String... c) {
        list = new ArrayList<>(Lists.transform(Arrays.asList(c), s -> new String(s)));
    }

    public Command(Collection<? extends CommandComponent> c) {
        list = new ArrayList<>(c);
    }

    /**
     * Transform an array of JS objects into a command line argument object
     *
     * @param array The input array
     * @return a valid {@linkplain sf.net.experimaestro.scheduler.Command} object
     */
    @Expose
    public static Command getCommand(List array) {
        final Command command = new Command();

        for (Object object : array) {
            final Command argument = new Command();
            StringBuilder sb = new StringBuilder();

            if (object instanceof CommandComponent) {
                command.add(object);
            } else {
                argumentWalkThrough(sb, argument, object);

                if (sb.length() > 0)
                    argument.add(sb.toString());

                command.add(argument);
            }

        }

        return command;
    }

    /**
     * Recursive parsing of the command line
     */
    private static void argumentWalkThrough(StringBuilder sb, Command command, Object object) {
        if (object == null)
            throw new IllegalArgumentException(java.lang.String.format("Null argument in command line"));

        if (object instanceof ScriptingPath)
            object = ((ScriptingPath) object).getObject();

        if (object instanceof java.nio.file.Path) {
            if (sb.length() > 0) {
                command.add(sb.toString());
                sb.delete(0, sb.length());
            }
            command.add(new Command.Path((java.nio.file.Path) object));
        } else if (object instanceof NativeArray) {
            for (Object child : (NativeArray) object)
                argumentWalkThrough(sb, command, JSUtils.unwrap(child));
        } else if (object instanceof JSParameterFile) {
            final JSParameterFile pFile = (JSParameterFile) object;
            command.add(new Command.ParameterFile(pFile.getKey(), pFile.getValue()));
        } else {
            sb.append(JSUtils.toString(object));
        }
    }

    @Override
    public void prepare(CommandContext environment) {
        list.forEach(Functional.propagate(c -> c.prepare(environment)));
    }

    /**
     * Iterates over all the command components
     *
     * @return An iterable
     */
    @Override
    public Stream<? extends CommandComponent> allComponents() {
        return list.parallelStream().flatMap(CommandComponent::allComponents);
    }

    @Override
    public Stream<Dependency> dependencies() {
        return list.stream().filter(c -> c instanceof SubCommand).flatMap(c -> ((SubCommand) c).dependencies());
    }

    public void forEachDependency(Consumer<Dependency> consumer) {
        for (CommandComponent c : list) {
            if (c instanceof SubCommand) {
                ((SubCommand) c).forEachDependency(consumer);
            }
        }
    }

    @Override
    public void forEachCommand(Consumer<? super AbstractCommand> consumer) {
        for (CommandComponent component : list) {
            component.forEachCommand(consumer);
        }
    }

    @Override
    public java.lang.String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (CommandComponent argument : list) {
            sb.append('\'');
            sb.append(argument.toString());
            sb.append('\'');
            if (first)
                first = false;
            else
                sb.append(',');
        }
        sb.append("]");
        return sb.toString();
    }

    public int size() {
        return list.size();
    }

    @Expose
    public void add(CommandComponent... arguments) {
        list.addAll(Arrays.asList(arguments));
    }

    @Expose("add_subcommand")
    public void addSubCommand(Commands commands) {
        list.add(new SubCommand(commands));
    }

    @Expose
    public void add(java.lang.String... arguments) {
        Arrays.asList(arguments).forEach(t -> add(new String(t)));
    }

    @Expose
    public void add(Object... arguments) {
        Arrays.asList(arguments).forEach(t -> {
            if (t instanceof CommandOutput) {
                // Creates a new command output to ensure we have a copy of the stream
                list.add(new CommandOutput(((CommandOutput) t).getCommand()));
            } else if (t instanceof CommandComponent) {
                list.add((CommandComponent) t);
            } else if (t instanceof java.nio.file.Path) {
                list.add(new Path((java.nio.file.Path) t));
            } else if (t instanceof JsonPath) {
                list.add(new Path(((JsonPath) t).get()));
            } else {
                list.add(new String(t.toString()));
            }
        });
    }

    public java.lang.String toString(CommandContext environment) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (CommandComponent component : list)
            sb.append(component.toString(environment));
        return sb.toString();
    }

    public ArrayList<CommandComponent> list() {
        return this.list;
    }


    /**
     * Used when the argument should be replaced by a pipe
     */
    @Exposed
    public static class CommandOutput implements CommandComponent, Serializable {
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
        public java.lang.String toString(CommandContext environment) throws IOException {
            final Object data = environment.getData(this);
            return environment.resolve((java.nio.file.Path) data);
        }

        @Override
        public void forEachCommand(Consumer<? super AbstractCommand> consumer) {
            consumer.accept(command);
            command.forEachCommand(consumer);
        }

        public AbstractCommand getCommand() {
            return command;
        }
    }

    public static class String implements CommandComponent, Serializable {
        java.lang.String string;

        private String() {
        }

        public String(java.lang.String string) {
            this.string = string;
        }

        @Override
        public java.lang.String toString(CommandContext environment) {
            return string;
        }

        @Override
        public java.lang.String toString() {
            return string;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            String string1 = (String) o;
            return Objects.equals(string, string1.string);
        }

        @Override
        public int hashCode() {
            return Objects.hash(string);
        }
    }

    public static class Path implements CommandComponent, Serializable {
        @JsonAdapter(JsonPathConverter.class)
        private java.nio.file.Path file;

        private Path() {
        }

        public Path(java.nio.file.Path file) {
            this.file = file;
        }

        @Override
        public java.lang.String toString(CommandContext environment) throws IOException {
            return environment.resolve(file);
        }

        @Override
        public java.lang.String toString() {
            return java.lang.String.format("Path{%s}", file.toUri());
        }
    }

    @Exposed
    public static class ParameterFile implements CommandComponent, Serializable {
        java.lang.String key;

        byte[] content;

        private ParameterFile() {
        }

        public ParameterFile(java.lang.String key, byte[] content) {
            this.key = key;
            this.content = content;
        }

        @Override
        public java.lang.String toString(CommandContext environment) throws IOException {
            java.nio.file.Path file = environment.getAuxiliaryFile(key, ".input");
            OutputStream out = Files.newOutputStream(file);
            out.write(content);
            out.close();

            return environment.resolve(file);
        }

        @Override
        public java.lang.String toString() {
            return java.lang.String.format("ParameterFile(%s)", key);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ParameterFile that = (ParameterFile) o;
            return Objects.equals(key, that.key) &&
                    Objects.equals(content, that.content);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, content);
        }
    }


    public static class WorkingDirectory implements CommandComponent {
        static final public WorkingDirectory INSTANCE = new WorkingDirectory();

        private WorkingDirectory() {
        }

        @Override
        public java.lang.String toString(CommandContext environment) throws IOException {
            return environment.getWorkingDirectory();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof WorkingDirectory;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Command command = (Command) o;
        return Objects.equals(list, command.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(list);
    }

    /**
     * A pipe
     */
    @Exposed
    static public class Pipe implements CommandComponent {
        static private Pipe PIPE = new Pipe();

        private Pipe() {
        }

        public static Pipe getInstance() {
            return PIPE;
        }
    }

    /**
     * A sub-command whose output / input can be globally set
     */
    @Exposed
    static public class SubCommand implements CommandComponent {
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


    static public class JsonParameterFile implements CommandComponent {
        private java.lang.String key;

        private Json json;

        private JsonParameterFile() {
        }

        public JsonParameterFile(java.lang.String key, Json json) {
            this.key = key;
            this.json = json;
        }

        @Override
        public java.lang.String toString(CommandContext environment) throws IOException {
            java.nio.file.Path file = environment.getAuxiliaryFile(key, ".json");
            try (OutputStream out = Files.newOutputStream(file);
                 OutputStreamWriter jsonWriter = new OutputStreamWriter(out)) {
                final JsonWriterOptions options = new JsonWriterOptions(ImmutableSet.of())
                        .ignore$(false)
                        .ignoreNull(false)
                        .simplifyValues(true)
                        .resolveFile(f -> {
                            try {
                                return environment.resolve(f);
                            } catch (IOException e) {
                                throw new XPMRuntimeException(e);
                            }
                        });
                json.writeDescriptorString(jsonWriter, options);
            } catch (IOException e) {
                throw new XPMRuntimeException(e, "Could not write JSON string for java task");
            }

            return environment.resolve(file);
        }
    }

    static public class Unprotected extends String {
        public Unprotected() {
        }

        public Unprotected(java.lang.String string) {
            super(string);
        }
    }
}
