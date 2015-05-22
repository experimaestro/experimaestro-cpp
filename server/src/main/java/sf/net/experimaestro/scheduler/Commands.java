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

import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.connectors.AbstractCommandBuilder;
import sf.net.experimaestro.utils.Graph;
import sf.net.experimaestro.utils.IdentityHashSet;
import sf.net.experimaestro.utils.Output;
import sf.net.experimaestro.utils.log.Logger;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A full command
 * <p>
 */
@Exposed
public class Commands extends AbstractCommand implements Iterable<AbstractCommand> {
    final static private Logger LOGGER = Logger.getLogger();
    /**
     * The list of commands status be executed
     * <p/>
     * The commands can refer status each other
     */
    ArrayList<AbstractCommand> commands = new ArrayList<>();

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
    public Commands(AbstractCommand... commands) {
        this.commands = new ArrayList<>(Arrays.asList(commands));
    }

    static private void fillEdges(IdentityHashSet<AbstractCommand> graph, Map<AbstractCommand, Set<AbstractCommand>> forward_edges, Map<AbstractCommand, Set<AbstractCommand>> backwards_edges, AbstractCommand command) {
        command.allComponents().forEach(argument -> {
            if (argument instanceof Command.CommandOutput) {
                final AbstractCommand subCommand = ((Command.CommandOutput) argument).getCommand();
                add(backwards_edges, command, subCommand);
                add(forward_edges, subCommand, command);

                graph.add(subCommand);
                fillEdges(graph, forward_edges, backwards_edges, subCommand);
            }
        });
    }

    static private void add(Map<AbstractCommand, Set<AbstractCommand>> map, AbstractCommand key, AbstractCommand value) {
        Set<AbstractCommand> set = map.get(key);
        if (set == null) {
            set = new IdentityHashSet<>();
            map.put(key, set);
        }
        set.add(value);
    }

    /**
     * Re-order the commands so that the dependencies are fulfilled
     */
    public ArrayList<AbstractCommand> reorder() {
        final IdentityHashSet<AbstractCommand> graph = new IdentityHashSet<>();
        Map<AbstractCommand, Set<AbstractCommand>> forward_edges = new IdentityHashMap<>();
        Map<AbstractCommand, Set<AbstractCommand>> backwards_edges = new IdentityHashMap<>();

        AbstractCommand previousCommand = null;

        for (AbstractCommand command : commands) {
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
        final ArrayList<AbstractCommand> ordered_objects = Graph.topologicalSort(graph, forward_edges, backwards_edges);
        if (graph.iterator().hasNext()) {
            final String s = Output.toString(", ", graph);
            LOGGER.error("Loop in command: %s", s);
            throw new IllegalArgumentException("Command has a loop [%s]");
        }

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

    public void forEachDependency(Consumer<Dependency> consumer) {
        // Process our dependencies
        for (Dependency dependency : dependencies) {
            consumer.accept(dependency);
        }

        commands.stream().forEach(c -> c.forEachDependency(consumer));
    }

    @Override
    public Iterator<AbstractCommand> iterator() {
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

    public void add(AbstractCommand command) {
        commands.add(command);
    }

    public void prepare(CommandContext env) {
        commands.forEach(c -> c.prepare(env));
    }

    @Override
    public Stream<? extends CommandComponent> allComponents() {
        return commands.stream().flatMap(AbstractCommand::allComponents);
    }

    public void forEachCommand(Consumer<? super AbstractCommand> consumer) {
        for(AbstractCommand command: commands) {
            consumer.accept(command);
            command.forEachCommand(consumer);
        }
    }

}
