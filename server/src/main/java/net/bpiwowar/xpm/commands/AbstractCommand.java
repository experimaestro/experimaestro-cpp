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

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.Dependency;
import net.bpiwowar.xpm.utils.Graph;
import net.bpiwowar.xpm.utils.IdentityHashSet;
import net.bpiwowar.xpm.utils.Output;
import net.bpiwowar.xpm.utils.UUIDObject;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * An abstract command
 */
@Exposed
public abstract class AbstractCommand implements Iterable<AbstractCommand>, UUIDObject {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * List of dependencies attached to this command
     * <p>
     * The dependencies are not saved during serialization since this will be handled
     * by the resource
     */
    protected transient ArrayList<Dependency> dependencies = new ArrayList<>();

    /**
     * Command UUID
     */
    transient private String uuid = UUID.randomUUID().toString();

    /**
     * The input redirect
     * <p>
     * Null indicates that the input should be the null device
     */
    Redirect inputRedirect = Redirect.INHERIT;

    /**
     * The output stream redirect.
     * <p>
     * Null indicates that the output should be discarded
     */
    Redirect outputRedirect = null;

    /**
     * The error stream redirect.
     * <p>
     * Null indicates that the output should be discarded
     */
    Redirect errorRedirect = Redirect.INHERIT;

    /**
     * Standard input
     * TODO: Should be in inputRedirect?
     */
    transient CommandOutput standardInput;

    /**
     * Process each dependency contained in a command or subcommand
     * @param consumer The consumer to be fed
     */
    final public void forEachDependency(Consumer<Dependency> consumer) {
        dependencies().forEach(consumer::accept);
    }

    /**
     * Prepare the command before the execution
     * @param env
     */
    void prepare(CommandContext env) {
        if (standardInput != null) {
            try {
                standardInput.prepare(env);
                inputRedirect = Redirect.from(standardInput.getFile(env));
            } catch (IOException e) {
                throw new XPMRuntimeException(e);
            }
        }
    }

    public AbstractCommand() {}

    public Redirect getOutputRedirect() {
        return outputRedirect;
    }

    public Redirect getErrorRedirect() {
        return errorRedirect;
    }

    @Expose
    public CommandOutput output() {
        return new CommandOutput(this);
    }

    public Stream<? extends AbstractCommandComponent> allComponents() {
        if (standardInput != null) {
            return Stream.of(standardInput);
        }
        return Stream.empty();
    }

    @Expose("add_dependency")
    public void addDependency(Dependency dependency) {
        dependencies.add(dependency);
    }

    public Stream<? extends Dependency> dependencies() {
        return dependencies.stream();
    }

    public void setStandardInput(CommandOutput standardInput) {
        this.standardInput = standardInput;
    }

    public CommandOutput getStandardInput() {
        return standardInput;
    }

    /**
     * Re-order the command so that the dependencies are fulfilled
     */
    public List<AbstractCommand> reorder() {
        final IdentityHashSet<AbstractCommand> graph = new IdentityHashSet<>();
        Map<AbstractCommand, Set<AbstractCommand>> forward_edges = new IdentityHashMap<>();
        Map<AbstractCommand, Set<AbstractCommand>> backwards_edges = new IdentityHashMap<>();

        AbstractCommand previousCommand = null;

        for (AbstractCommand command : this) {
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

    protected static void addDependency(IdentityHashSet<AbstractCommand> graph, Map<AbstractCommand, Set<AbstractCommand>> forward_edges, Map<AbstractCommand, Set<AbstractCommand>> backwards_edges, AbstractCommand to, AbstractCommand from) {
        add(backwards_edges, to, from);
        add(forward_edges, from, to);
        graph.add(from);
        fillEdges(graph, forward_edges, backwards_edges, from);
    }


    public boolean needsProtection() {
        return false;
    }

    /**
     * Return a streams of all command
     * @return A stream of all the command contained in this command (this command included)
     */
    public Stream<AbstractCommand> commands() {
        if (standardInput != null) {
            return Stream.of(standardInput.command);
        }
        return Stream.empty();
    }

    /**
     * Simplify the command
     * @return A simplified command
     */
    public AbstractCommand simplify() {
        return this;
    }

    /**
     * Copy our settings to a new command
     * @param command The command
     */
    protected void copyToCommand(AbstractCommand command) {
        dependencies.forEach(command::addDependency);
        if (command.getStandardInput() == null) {
            command.setStandardInput(getStandardInput());
        }

        if (command.outputRedirect == null) {
            command.outputRedirect = outputRedirect;
        }
        if (command.errorRedirect == null) {
            command.errorRedirect = errorRedirect;
        }
    }

    public void setErrorRedirect(Redirect errorRedirect) {
        this.errorRedirect = errorRedirect;
    }

    public void setOutputRedirect(Redirect outputRedirect) {
        this.outputRedirect = outputRedirect;
    }

    @Override
    public String getUUID() {
        return uuid;
    }

    @Override
    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public void postJSONSave(JsonWriter out) throws IOException {
        if (standardInput != null) {
            out.name("standardInput");
            out.value(standardInput.getUUID());
        }
    }

    @Override
    public void postJSONLoad(Map<String, UUIDObject> map, JsonReader in, String name) throws IOException {
        switch(name) {
            case "standardInput":
                standardInput = (CommandOutput) map.get(in.nextString());
                break;
        }
    }
}