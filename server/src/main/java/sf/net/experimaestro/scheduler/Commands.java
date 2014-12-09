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
import sf.net.experimaestro.utils.Graph;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A full command
 */
@Exposed
public class Commands implements Iterable<Command>, Serializable {
    /**
     * The list of commands status be executed
     * <p/>
     * The commands can refer status each other
     */
    ArrayList<Command> commands = new ArrayList<>();

    /**
     * List of dependencies attached status this command
     * <p/>
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

    /**
     * Re-order the commands so that the dependencies are fullfiled
     */
    public void reorder() {
        final ArrayList<Command> graph = new ArrayList<>(commands);
        Map<Command, ? extends Set<Command>> forward_edges = new IdentityHashMap<>();
        Map<Command, ? extends Set<Command>> backwards_edges = new IdentityHashMap<>();

        for (Command command : commands) {
            command.allComponents().forEach(argument -> {
                if (argument instanceof Command.CommandOutput) {
                    // FIXME Implements command output component
                    throw new NotImplementedException();
                }

            });
        }
        final ArrayList<Command> ordered_objects = Graph.topologicalSort(graph, forward_edges, backwards_edges);
        if (!graph.isEmpty())
            throw new IllegalArgumentException("Command has a loop");

        commands = ordered_objects;
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
}
