package net.bpiwowar.xpm.commands;

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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.bpiwowar.xpm.scheduler.Dependency;
import org.mozilla.javascript.NativeArray;
import net.bpiwowar.xpm.manager.js.JSParameterFile;
import net.bpiwowar.xpm.manager.json.JsonPath;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.manager.scripting.ScriptingPath;
import net.bpiwowar.xpm.utils.Functional;
import net.bpiwowar.xpm.utils.JSUtils;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.IOException;
import java.io.Serializable;
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
        list = new ArrayList<>(Lists.transform(Arrays.asList(c), s -> new CommandString(s)));
    }

    public Command(Collection<? extends CommandComponent> c) {
        list = new ArrayList<>(c);
    }

    /**
     * Transform an array of JS objects into a command line argument object
     *
     * @param array The input array
     * @return a valid {@linkplain Command} object
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
            command.add(new CommandPath((java.nio.file.Path) object));
        } else if (object instanceof NativeArray) {
            for (Object child : (NativeArray) object)
                argumentWalkThrough(sb, command, JSUtils.unwrap(child));
        } else if (object instanceof JSParameterFile) {
            final JSParameterFile pFile = (JSParameterFile) object;
            command.add(new ParameterFile(pFile.getKey(), pFile.getValue()));
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
        return list.stream().filter(c -> c instanceof SubCommand).flatMap(c -> c.dependencies());
    }

    public ArrayList<CommandComponent> components() {
        return list;
    }

    @Override
    public List<AbstractCommand> reorder() {
        return ImmutableList.of(this);
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
        Arrays.asList(arguments).forEach(t -> add(new CommandString(t)));
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
                list.add(new CommandPath((java.nio.file.Path) t));
            } else if (t instanceof JsonPath) {
                list.add(new CommandPath(((JsonPath) t).get()));
            } else {
                list.add(new CommandString(t.toString()));
            }
        });
    }

    public java.lang.String toString(CommandContext environment) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (CommandComponent component : list)
            sb.append(component.toString(environment));
        return sb.toString();
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


}
