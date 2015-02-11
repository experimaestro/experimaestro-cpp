/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.scheduler;

import com.google.common.collect.ImmutableSet;
import com.sleepycat.persist.model.Persistent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.annotations.Expose;
import sf.net.experimaestro.annotations.Exposed;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonWriterOptions;
import sf.net.experimaestro.utils.Streams;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A command line argument (or argument part)
 *
 * @author B. Piwowarski
 */
@Persistent
@Exposed
public class Command extends AbstractCommand implements CommandComponent {
    public final static Logger LOGGER = Logger.getLogger();

    /**
     * The list of components in this command
     */
    ArrayList<CommandComponent> list;

    @Expose
    public Command() {
        list = new ArrayList<>();
    }

    public Command(Collection<? extends CommandComponent> c) {
        list = new ArrayList<>(c);
    }

    @Override
    public void prepare(CommandEnvironment environment) {
        list.forEach(Streams.propagate(c -> c.prepare(environment)));
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

    public void forEachDependency(Consumer<Dependency> consumer) {
        for (CommandComponent c : list) {
            if (c instanceof SubCommand) {
                ((SubCommand) c).forEachDependency(consumer);
            }
        }
    }

    @Override
    public void forEachCommand(Consumer<? super AbstractCommand> consumer) {
        for(CommandComponent component: list) {
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
            } else if (t instanceof FileObject) {
                list.add(new Path((FileObject) t));
            } else {
                list.add(new String(t.toString()));
            }
        });
    }

    public java.lang.String toString(CommandEnvironment environment) throws IOException {
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
    @Persistent
    public static class CommandOutput implements CommandComponent {
        /**
         * The output
         */
        AbstractCommand command;

        protected CommandOutput() {}

        public CommandOutput(AbstractCommand command) {
            this.command = command;
        }



        @Override
        public void prepare(CommandEnvironment environment) throws FileSystemException {
            final FileObject file = environment.getUniqueFile("command", ".pipe");
            final Object o = environment.setData(this, file);
            if (o != null) throw new RuntimeException("CommandOutput data should be null");
            command.outputRedirects.add(file);
            environment.detached(command, true);
        }

        @Override
        public java.lang.String toString(CommandEnvironment environment) throws FileSystemException {
            return environment.resolve((FileObject) environment.getData(this));
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

    @Persistent
    public static class String implements CommandComponent {
        java.lang.String string;

        private String() {
        }

        public String(java.lang.String string) {
            this.string = string;
        }

        @Override
        public java.lang.String toString(CommandEnvironment environment) {
            return string;
        }

        @Override
        public java.lang.String toString() {
            return string;
        }
    }

    @Persistent
    public static class Path implements CommandComponent {
        /**
         * An URI
         */
        private java.lang.String filename;

        private Path() {
        }

        public Path(FileObject file) {
            filename = file.getName().getURI();
        }

        public Path(java.lang.String filename) {
            this.filename = filename;
        }

        @Override
        public java.lang.String toString(CommandEnvironment environment) throws FileSystemException {
            FileObject object = Scheduler.getVFSManager().resolveFile(filename);
            return environment.resolve(object);
        }

        @Override
        public java.lang.String toString() {
            return java.lang.String.format("<xp:path>%s</xp:path>", filename);
        }
    }

    @Persistent
    @Exposed
    public static class ParameterFile implements CommandComponent {
        java.lang.String key;
        byte[] content;

        private ParameterFile() {
        }

        public ParameterFile(java.lang.String key, byte[] content) {
            this.key = key;
            this.content = content;
        }

        @Override
        public java.lang.String toString(CommandEnvironment environment) throws IOException {
            FileObject file = environment.getAuxiliaryFile(key, ".input");
            OutputStream out = file.getContent().getOutputStream();
            out.write(content);
            out.close();

            return environment.resolve(file);
        }

        @Override
        public java.lang.String toString() {
            return java.lang.String.format("ParameterFile(%s)", key);
        }
    }


    @Persistent
    public static class WorkingDirectory implements CommandComponent {
        static final public WorkingDirectory INSTANCE = new WorkingDirectory();

        private WorkingDirectory() {
        }

        @Override
        public java.lang.String toString(CommandEnvironment environment) throws IOException {
            return environment.getWorkingDirectory();
        }
    }

    /**
     * A pipe
     */
    @Persistent
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
    @Persistent
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
        public void prepare(CommandEnvironment environment) {
            commands.prepare(environment);
        }
    }


    @Persistent
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
        public java.lang.String toString(CommandEnvironment environment) throws IOException {
            FileObject file = environment.getAuxiliaryFile(key, ".json");
            try (OutputStream out = file.getContent().getOutputStream();
                 OutputStreamWriter jsonWriter = new OutputStreamWriter(out)) {
                final JsonWriterOptions options = new JsonWriterOptions(ImmutableSet.of())
                        .ignore$(false)
                        .ignoreNull(false)
                        .simplifyValues(true)
                        .resolveFile(f -> {
                            try {
                                return environment.resolve(f);
                            } catch (FileSystemException e) {
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
}
