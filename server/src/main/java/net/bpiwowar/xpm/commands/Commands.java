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

import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.Dependency;
import net.bpiwowar.xpm.utils.Graph;
import net.bpiwowar.xpm.utils.IdentityHashSet;
import net.bpiwowar.xpm.utils.Output;
import net.bpiwowar.xpm.utils.log.Logger;

import java.util.*;
import java.util.stream.Stream;

/**
 * A full command
 * <p>
 */
@Exposed
public class Commands extends AbstractCommand  {
    final static private Logger LOGGER = Logger.getLogger();
    /**
     * The list of command status be executed
     * <p/>
     * The command can refer status each other
     */
    ArrayList<AbstractCommand> commands = new ArrayList<>();

    /**
     * Default constructor (for DB serialization)
     */
    public Commands() {
    }

    /**
     * Construct with a set of command
     */
    public Commands(AbstractCommand... commands) {
        this.commands = new ArrayList<>(Arrays.asList(commands));
    }

    static private void fillEdges(IdentityHashSet<AbstractCommand> graph, Map<AbstractCommand, Set<AbstractCommand>> forward_edges, Map<AbstractCommand, Set<AbstractCommand>> backwards_edges, AbstractCommand command) {
        command.allComponents().forEach(argument -> {
            if (argument instanceof CommandOutput) {
                final AbstractCommand subCommand = ((CommandOutput) argument).getCommand();
                addDependency(graph, forward_edges, backwards_edges, command, subCommand);
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
     * Re-order the command so that the dependencies are fulfilled
     */
    public List<AbstractCommand> reorder() {
        final IdentityHashSet<AbstractCommand> graph = new IdentityHashSet<>();
        Map<AbstractCommand, Set<AbstractCommand>> forward_edges = new IdentityHashMap<>();
        Map<AbstractCommand, Set<AbstractCommand>> backwards_edges = new IdentityHashMap<>();

        AbstractCommand previousCommand = null;

        for (AbstractCommand command : commands) {
            // Adds constraints on the graph: the order of the command should be respected
            if (previousCommand != null) {
                add(forward_edges, previousCommand, command);
                add(backwards_edges, command, previousCommand);
            }
            graph.add(command);
            previousCommand = command;
            
            // Add all edges
            fillEdges(graph, forward_edges, backwards_edges, command);
        }
        if (getStandardInput() != null) {
            addDependency(graph, forward_edges, backwards_edges, this, getStandardInput().command);
        }
        final ArrayList<AbstractCommand> ordered_objects = Graph.topologicalSort(graph, forward_edges, backwards_edges);
        if (graph.iterator().hasNext()) {
            final String s = Output.toString(", ", graph);
            LOGGER.error("Loop in command: %s", s);
            throw new IllegalArgumentException("Command has a loop");
        }

        return ordered_objects;
    }

    protected static void addDependency(IdentityHashSet<AbstractCommand> graph, Map<AbstractCommand, Set<AbstractCommand>> forward_edges, Map<AbstractCommand, Set<AbstractCommand>> backwards_edges, AbstractCommand to, AbstractCommand from) {
        add(backwards_edges, to, from);
        add(forward_edges, from, to);
        graph.add(from);
        fillEdges(graph, forward_edges, backwards_edges, from);
    }

    public Stream<Dependency> dependencies() {
        // Process our dependencies
        return Stream.concat(super.dependencies(), commands.stream().flatMap(AbstractCommand::dependencies));
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
                "command=" + commands +
                '}';
    }

    public void add(AbstractCommand command) {
        commands.add(command);
    }

    public void prepare(CommandContext env) {
        super.prepare(env);
        commands.forEach(c -> c.prepare(env));
    }

    @Override
    public Stream<? extends CommandComponent> allComponents() {
        return Stream.concat(super.allComponents(), commands.stream().flatMap(AbstractCommand::allComponents));
    }



    public void addUnprotected(String command) {
        add(new Command(new Unprotected(command)));
    }

    public AbstractCommand get(int i) {
        return commands.get(i);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Commands that = (Commands) o;
        return Objects.equals(commands, that.commands) &&
                Objects.equals(dependencies, that.dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commands, dependencies);
    }

    @Override
    public boolean needsProtection() {
        return commands.size() > 1;
    }

    @Override
    public Stream<AbstractCommand> commands() {
        return Stream.concat(Stream.of(this), commands.stream().flatMap(AbstractCommand::commands));
    }

    /**
     * Simplify the command
     * @return A simplified command
     */
    public AbstractCommand simplify() {
        for (int i = 0; i < commands.size(); i++) {
            commands.set(i, commands.get(i).simplify());
        }

        if (commands.size() == 1) {
            // Copy dependencies
            final AbstractCommand command = commands.get(0);
            copyToCommand(command);
            return command;
        }
        return this;
    }

}
