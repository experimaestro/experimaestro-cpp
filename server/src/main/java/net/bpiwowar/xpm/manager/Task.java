package net.bpiwowar.xpm.manager;

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

import net.bpiwowar.xpm.commands.AbstractCommand;
import net.bpiwowar.xpm.commands.CommandOutput;
import net.bpiwowar.xpm.commands.Redirect;
import net.bpiwowar.xpm.exceptions.ExperimaestroException;
import net.bpiwowar.xpm.exceptions.NoSuchParameter;
import net.bpiwowar.xpm.exceptions.ValueMismatchException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.manager.scripting.ScriptContext;
import net.bpiwowar.xpm.commands.Commands;
import net.bpiwowar.xpm.utils.Graph;
import net.bpiwowar.xpm.utils.JSUtils;
import net.bpiwowar.xpm.utils.log.Logger;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * The abstract TaskReference object
 */
@Exposed
public abstract class Task {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The information related to this class of experiment
     */
    protected TaskFactory factory;

    /**
     * List of sub-tasks
     */
    protected Map<String, Value> values = new TreeMap<>();

    /**
     * Sub-tasks without name (subset of the map {@link #values}).
     */
    protected Set<String> notNamedValues = new TreeSet<>();

    protected Task() {
    }

    /**
     * Construct a new task from a {@link TaskFactory}
     *
     * @param information
     */
    protected Task(TaskFactory information) {
        this.factory = information;
    }

    static private void addEdge(Map<String, TreeSet<String>> edges,
                                String from, String to) {
        TreeSet<String> inSet = edges.get(from);
        if (inSet == null)
            edges.put(from, inSet = new TreeSet<String>());
        inSet.add(to);
    }

    /**
     * Returns the factory that created this task
     */
    public TaskFactory getFactory() {
        return factory;
    }

    /**
     * Get a specific input
     *
     * @param key
     * @return
     */
    protected Input getInput(String key) {
        return getInputs().get(DotName.parse(key));
    }

    /**
     * Get the list of inputs
     */
    public Map<String, Input> getInputs() {
        return factory.getInputs();
    }

    /**
     * Get the current outputs (given the current parameters)
     */
    public Type getOutput() {
        return factory.getOutput();
    }

    /**
     * Get the list of set values
     *
     * @return A map or null
     */
    public Map<String, Value> getValues() {
        return values;
    }

    /**
     * Run this task. The output is a valid XML document where top level
     * elements correspond to the different outputs generated by the method
     *
     * @param taskContext
     * @return An XML description of the output
     */
    public abstract Json doRun(ScriptContext taskContext);

    /**
     * Run this task.
     * <p>
     * Calls {@linkplain #doRun(ScriptContext)}
     *
     * @param taskContext
     */
    final public Json run(ScriptContext taskContext, Parameters... parameters) throws NoSuchParameter, ValueMismatchException {
        LOGGER.debug("Running task [%s]", factory == null ? "n/a" : factory.id);
        processInputs(taskContext);

        // Do the real-run
        Json json = doRun(taskContext);
        return json;

    }

    protected void processInputs(ScriptContext taskContext) throws NoSuchParameter, ValueMismatchException {
        // (1) Get the inputs so that dependent ones are evaluated latter
        ArrayList<String> list = getOrderedInputs();

        // (2) Do some post-processing on values
        for (String key : list) {
            // Get some more information
            Input input = factory.getInputs().get(key);
            Value value = values.get(key);

            try {

                // Process connection
                try {
                    value.processConnections(this);
                } catch (XPMRuntimeException e) {
                    e.addContext("While connecting from [%s] in task [%s]", key, factory.id);
                    throw e;
                }

                // Process the value (run the task, etc.)
                value.process(taskContext);

                // Check if value was required

                if (!input.isOptional()) {
                    if (!value.isSet())
                        throw new XPMRuntimeException("Parameter [%s] is not set for task [%s]", key, factory.id);
                }

                // Check type
                final Type type = input.getType();

                if (type != null && value.isSet()) {
                    Json element = value.get();
                    assert element != null;
                    type.validate(element);
                }
            } catch (XPMRuntimeException | ExperimaestroException e) {
                e.addContext("While processing input [%s] in task [%s]", key, factory.id);
                throw e;
            } catch (RuntimeException e) {
                XPMRuntimeException xpmException = new XPMRuntimeException(e);
                xpmException.addContext("While processing input [%s] in task [%s]", key, factory.id);
                throw xpmException;
            }

        }
    }

    /**
     * Order the inputs in topological order in order to evaluate them when
     * dependencies due to connections are satisfied
     */
    private ArrayList<String> getOrderedInputs() {
        // (1) Order the values to avoid dependencies
        // See http://en.wikipedia.org/wiki/Topological_sorting
        ArrayList<String> nodes = new ArrayList<>(values.keySet());

        Map<String, TreeSet<String>> forward_edges = new TreeMap<>();
        Map<String, TreeSet<String>> backwards_edges = new TreeMap<>();


        // Build the edge maps
        for (Entry<String, Value> entry : values.entrySet()) {
            final String to = entry.getKey();
            final Input input = entry.getValue().input;
            if (input.connections != null)
                for (Connection connection : input.connections)
                    for (String from : connection.inputs()) {
                        LOGGER.debug("[build] Adding edge from %s to %s", from, to);
                        addEdge(forward_edges, from, to);
                        addEdge(backwards_edges, to, from);
                    }
        }

        // Get the free nodes
        ArrayList<String> sorted_nodes = Graph.topologicalSort(nodes, forward_edges, backwards_edges);
        if (!nodes.isEmpty())
            throw new XPMRuntimeException("Loop in the graph for task [%s]",
                    factory.id);

        return sorted_nodes;
    }

    /**
     * Set a parameter from an XML value
     *
     * @param id    The identifier for this parameter (dot names)
     * @param value The value to be set (this should be an XML fragment)
     * @return True if the parameter was set and false otherwise
     */
    public final void setParameter(DotName id, Json value) throws NoSuchParameter {
        try {
            getValue(id).set(value.seal());
        } catch (XPMRuntimeException e) {
            e.addContext("While setting parameter %s of %s", id, factory.getId());
            throw e;
        } catch (RuntimeException e) {
            final XPMRuntimeException e2 = new XPMRuntimeException(e);
            e2.addContext("While setting parameter %s of %s", id, factory == null ? "[null]" : factory.getId());
            throw e2;
        }
    }

    /**
     * Set a parameter from a text value.
     * <p>
     * Wraps the value into a node whose name depends upon the input
     *
     * @param id
     * @param value
     */
    public void setParameter(DotName id, String value) throws NoSuchParameter {
        final Value v = getValue(id);
        final Json doc = ValueType.wrapString(value, null);
        v.set(doc);
    }

    /**
     * Returns the {@linkplain} object corresponding to the
     *
     * @param id
     * @return
     * @throws NoSuchParameter
     */
    public final Value getValue(DotName id) throws NoSuchParameter {
        String name = id.get(0);

        // Look at merged inputs
        if (!notNamedValues.isEmpty()) {
            for (String vname : notNamedValues) {
                try {
                    Value v = values.get(vname);
                    return v.getValue(id);
                } catch (NoSuchParameter e) {
                }
            }

        }

        // Look at our inputs
        Value inputValue = values.get(name);
        if (inputValue == null)
            throw new NoSuchParameter("Task %s has no input [%s]", factory.id,
                    name);

        return inputValue.getValue(id.offset(1));
    }

    /**
     * Initialise the task
     */
    public void init() {
        // Create values for each input
        for (Entry<String, Input> entry : getInputs().entrySet()) {
            String key = entry.getKey();
            final Value value = entry.getValue().newValue();
            values.put(key, value);

            // Add to the unnamed options
            if (entry.getValue().isUnnamed())
                notNamedValues.add(entry.getKey());
        }

    }

    /**
     * Returns a deep copy of this task
     *
     * @return A new TaskReference
     */
    final public Task copy() {
        try {
            Constructor<? extends Task> constructor = this.getClass()
                    .getConstructor();
            Task copy = constructor.newInstance();
            copy.init(this);
            return copy;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new XPMRuntimeException(t);
        }
    }

    /**
     * Initialise the TaskReference from another one
     * <p>
     * This method is called right after object creation in {@link #copy()}
     *
     * @param other The task to copy data from
     */
    protected void init(Task other) {
        // Copy the factory
        factory = other.factory;

        // shallow copy for this field, since it won't change
        notNamedValues = other.notNamedValues;

        // Deep copy
        for (Entry<String, Value> entry : other.values.entrySet()) {
            values.put(entry.getKey(), entry.getValue().copy());
        }
    }

    @Expose(value = "set")
    public void set(String id, Object value) throws NoSuchParameter {
        DotName qid = DotName.parse(id);
        setParameter(qid, ValueType.wrap(JSUtils.unwrap(value)));
    }

    @Expose("run")
    public Json run(boolean simulate, Parameters... parameters) throws ValueMismatchException, NoSuchParameter {
        try (final ScriptContext scriptContext = ScriptContext.get().copy()) {
            IdentityHashMap<Object, Parameters> pmap = new IdentityHashMap<>();
            Stream.of(parameters).forEach(p -> pmap.put(p.getKey(), p));

            ScriptContext.get().simulate(simulate);
            return run(scriptContext);
        }
    }

    @Expose("run")
    public Json run(Parameters... parameters) throws ValueMismatchException, NoSuchParameter {
        return run(false, parameters);
    }

    public AbstractCommand commands(IdentityHashMap<Object, Parameters> parameters) throws ValueMismatchException, NoSuchParameter {
        final Commands commands = new Commands();

        // Add streams and dependencies
        final HashMap<Object, CommandOutput> streams = new HashMap<>();

        getValues().values().stream()
                .map(e -> e.get()).filter(e -> e instanceof JsonTask)
                .forEach(e -> {
                    final JsonTask jsonTask = (JsonTask) e;
                    // Add dependencies for these command
                    final AbstractCommand subcommand = jsonTask.getCommand();
                    commands.dependencies().forEach(commands::addDependency);

                    // Add the command
                    commands.add(subcommand);
                    subcommand.setOutputRedirect(null);
                    streams.put(null, subcommand.output());
                });


        final Commands taskCommands = _commands(streams, parameters);
        taskCommands.setOutputRedirect(Redirect.INHERIT);

        commands.add(taskCommands);

        final CommandOutput standardInput = streams.get(null);
        if (standardInput != null) {
            commands.setStandardInput(standardInput);
        }

        return commands.simplify();
    }

    protected Commands _commands(HashMap<Object, CommandOutput> streams, IdentityHashMap<Object, Parameters> parameters) throws ValueMismatchException, NoSuchParameter {
        throw new UnsupportedOperationException("Cannot return command for a task of type " + this.getClass());
    }

    /**
     * Returns a JSON that correspond to the parameters to be transmitted to
     * an external task
     *
     * @return A JSON object
     */
    public JsonObject getInputsAsJson() {
        JsonObject json = new JsonObject();
        values.forEach((key, value) -> {
            json.put(key, value.getAsInput());
        });
        return json;
    }

    public JsonObject getOutputJson() {
        JsonObject json = new JsonObject();
        values.forEach((key, value) -> {
            final Input input = factory.getInputs().get(key);
            final String copyTo = input.getCopyTo();
            if (copyTo != null && !copyTo.isEmpty()) key = copyTo;
            json.put(key, value.get());
        });
        return json;
    }

    public Value getValue(String key) {
        return getValues().get(key);
    }
}
