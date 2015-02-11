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

import sf.net.experimaestro.annotations.Expose;
import sf.net.experimaestro.annotations.Exposed;
import sf.net.experimaestro.connectors.AbstractCommandBuilder;
import sf.net.experimaestro.utils.Graph;
import sf.net.experimaestro.utils.IdentityHashSet;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A full command
 * <p>
 * TODO: commands should be a subclass of Command
 */
@Exposed
public class Commands implements Iterable<Command> {
    /**
     * The list of commands status be executed
     * <p/>
     * The commands can refer status each other
     */
    ArrayList<Command> commands = new ArrayList<>();

    /**
     * List of dependencies attached to this command
     * <p>
     * The dependencies are not saved during serialization since this will be handled
     * by the resource
     */
    transient private ArrayList<Dependency> dependencies = new ArrayList<>();

    /**
     * Default constructor (for DB serialization)
     */
    public Commands() {
    }

    /**
     * Construct with a set of commands
     */
    public Commands(Command... commands) {
        this.commands = new ArrayList<>(Arrays.asList(commands));
    }

    static private void fillEdges(IdentityHashSet<Command> graph, Map<Command, Set<Command>> forward_edges, Map<Command, Set<Command>> backwards_edges, Command command) {
        command.allComponents().forEach(argument -> {
            if (argument instanceof Command.CommandOutput) {
                final Command subCommand = ((Command.CommandOutput) argument).getCommand();
                add(backwards_edges, command, subCommand);
                add(forward_edges, subCommand, command);

                graph.add(subCommand);
                fillEdges(graph, forward_edges, backwards_edges, subCommand);
            }
        });
    }

    static private void add(Map<Command, Set<Command>> map, Command key, Command value) {
        Set<Command> set = map.get(key);
        if (set == null) {
            set = new IdentityHashSet<>();
            map.put(key, set);
        }
        set.add(value);
    }

    /**
     * Re-order the commands so that the dependencies are fulfilled
     */
    public ArrayList<Command> reorder() {
        final IdentityHashSet<Command> graph = new IdentityHashSet<>();
        Map<Command, Set<Command>> forward_edges = new IdentityHashMap<>();
        Map<Command, Set<Command>> backwards_edges = new IdentityHashMap<>();

        Command previousCommand = null;

        for (Command command : commands) {
            // Adds constraints on the graph: the order of the commands should be respected
            if (previousCommand != null) {
                add(forward_edges, previousCommand, command);
                add(backwards_edges, command, previousCommand);
            }
            graph.add(command);
            previousCommand = command;

            // Add the command to
            if (command.outputRedirect == null)
                command.outputRedirect = AbstractCommandBuilder.Redirect.INHERIT;
            if (command.errorRedirect == null)
                command.errorRedirect = AbstractCommandBuilder.Redirect.INHERIT;
            if (command.inputRedirect == null)
                command.inputRedirect = AbstractCommandBuilder.Redirect.INHERIT;

            fillEdges(graph, forward_edges, backwards_edges, command);
        }
        final ArrayList<Command> ordered_objects = Graph.topologicalSort(graph, forward_edges, backwards_edges);
        if (!graph.isEmpty())
            throw new IllegalArgumentException("Command has a loop");

        return ordered_objects;
    }

    @Expose("add_dependency")
    public void addDependency(Dependency dependency) {
        dependencies.add(dependency);
    }

    public Stream<Dependency> dependencies() {
        // Process our dependencies
        return Stream.concat(dependencies.stream(), commands.stream().flatMap(c -> c.dependencies()));
    }

    @Override
    public Iterator<Command> iterator() {
        return commands.iterator();
    }

    public int size() {
        return commands.size();
    }

    @Override
    public String toString() {
        return "Commands{" +
                "commands=" + commands +
                '}';
    }

    public void add(Command command) {
        commands.add(command);
    }

    public void prepare(CommandEnvironment env) {
        commands.forEach(c -> c.prepare(env));
    }

    public void forEachCommand(Consumer<? super Command> consumer) {
        for(Command command: commands) {
            consumer.accept(command);
            command.forEachCommand(consumer);
        }
    }
}
