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

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.Dependency;
import net.bpiwowar.xpm.utils.Functional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A command line argument (or argument part)
 *
 * @author B. Piwowarski
 */
@Exposed
public class Command extends AbstractCommand implements AbstractCommandComponent, Serializable {
    public final static Logger LOGGER = LogManager.getFormatterLogger();

    /**
     * The list of components in this command
     */
    ArrayList<AbstractCommandComponent> list;

    @Expose
    public Command() {
        list = new ArrayList<>();
    }

    public Command(AbstractCommandComponent... c) {
        list = new ArrayList<>(Arrays.asList(c));
    }
    public Command(java.lang.String... c) {
        list = new ArrayList<>(Lists.transform(Arrays.asList(c), s -> new CommandString(s)));
    }

    public Command(Collection<? extends AbstractCommandComponent> c) {
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

            if (object instanceof AbstractCommandComponent) {
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
            throw new IllegalArgumentException("Null argument in command line");

        if (object instanceof java.nio.file.Path) {
            if (sb.length() > 0) {
                command.add(sb.toString());
                sb.delete(0, sb.length());
            }
            command.add(new CommandPath((java.nio.file.Path) object));
        }  else {
            sb.append(object.toString());
        }
    }

    @Override
    public void prepare(CommandContext environment) {
        super.prepare(environment);
        list.forEach(Functional.propagate(c -> c.prepare(environment)));
    }

    /**
     * Iterates over all the command components
     *
     * @return An iterable
     */
    @Override
    public Stream<? extends AbstractCommandComponent> allComponents() {
        return Stream.concat(super.allComponents(), list.parallelStream().flatMap(AbstractCommandComponent::allComponents));
    }

    @Override
    public Stream<Dependency> dependencies() {
        return Stream.concat(super.dependencies(), list.stream().flatMap(c -> c.dependencies()));
    }

    public ArrayList<AbstractCommandComponent> components() {
        return list;
    }

    @Override
    public java.lang.String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (AbstractCommandComponent argument : list) {
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
    public void add(AbstractCommandComponent... arguments) {
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
            } else if (t instanceof AbstractCommandComponent) {
                list.add((AbstractCommandComponent) t);
            } else if (t instanceof java.nio.file.Path) {
                list.add(new CommandPath((java.nio.file.Path) t));
            } else {
                list.add(new CommandString(t.toString()));
            }
        });
    }

    public java.lang.String toString(CommandContext environment) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (AbstractCommandComponent component : list)
            sb.append(component.toString(environment));
        return sb.toString();
    }

    @Override
    public Iterator<AbstractCommand> iterator() {
        return Iterators.singletonIterator(this);
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

    @Override
    public Stream<AbstractCommand> commands() {
        return Stream.concat(Stream.concat(super.commands(), Stream.of(this)), components().stream().flatMap(AbstractCommandComponent::commands));
    }
}
