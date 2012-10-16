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

package sf.net.experimaestro.manager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.exceptions.NoSuchParameter;
import sf.net.experimaestro.plan.ParseException;
import sf.net.experimaestro.plan.PlanParser;
import sf.net.experimaestro.utils.Output;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.Map.Entry;

/**
 * The abstract Task object
 */
public abstract class Task {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The information related to this class of experiment
     */
    protected TaskFactory factory;

    /**
     * List of sub-tasks
     */
    protected Map<String, Value> values = new TreeMap<String, Value>();

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
        return getInputs().get(new DotName(key));
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
     * @return An XML description of the output
     */
    public abstract Document doRun();

    /**
     * Run this task.
     * <p/>
     * Calls {@linkplain #doRun()}
     */
    final public Document run() {
        LOGGER.info("Running task [%s]", factory == null ? "n/a" : factory.id);

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
                } catch (ExperimaestroRuntimeException e) {
                    e.addContext("While connecting from [%s] in task [%s]", key, factory.id);
                    throw e;
                }

                // Process the value (run the task, etc.)
                value.process();

                // Check if value was required

                if (!input.isOptional()) {
                    if (!value.isSet())
                        throw new ExperimaestroRuntimeException("Parameter [%s] is not set for task [%s]", key, factory.id);
                }

                // Check type
                final Type type = input.getType();
                if (type != null && value.isSet()) {
                    final Element element = value.get().getDocumentElement();

                    // Check if we have the expected element type
                    final String tagName = element.getLocalName();
                    final String tagURI = element.getNamespaceURI();

                    if (!tagURI.equals(type.getNamespaceURI()) || !tagName.equals(type.getLocalPart()))
                        throw new ExperimaestroRuntimeException("Parameter [%s] in task [%s] was set to a value with a wrong type [{%s}%s] - expected [%s]",
                                key, factory.id, tagURI, tagName, type);

                    // In cases of values, further check
                    if (type instanceof ValueType) {
                        QName valueType = ((ValueType) type).getValueType();
                        String x = element.getAttribute("value");
                        boolean ok = false;

                        // Test if the value is OK
                        switch (valueType.getNamespaceURI()) {
                            case Manager.XMLSCHEMA_NS:
                                switch (valueType.getLocalPart()) {
                                    case "string":
                                        ok = true;
                                        break; // we accepts anything
                                    case "float":
                                        Float.parseFloat(x);
                                        ok = true;
                                        break;
                                    case "integer":
                                        Integer.parseInt(x);
                                        ok = true;
                                        break;
                                }
                                break;

                            case Manager.EXPERIMAESTRO_NS:
                                switch (valueType.getLocalPart()) {
                                    case "directory":
                                        LOGGER.warn("Did not check if [%s] was a directory", x);
                                        ok = true;
                                        break;
                                    case "file":
                                        LOGGER.warn("Did not check if [%s] was a file", x);
                                        ok = true;
                                        break;
                                }
                                break;
                        }

                        if (!ok)
                            throw new ExperimaestroRuntimeException("Un-handled type [%s] in processing parameter [%s] of task [%s]",
                                    valueType, key, factory.id);

                    }
                }
            } catch (ExperimaestroRuntimeException e) {
                e.addContext("While processing input [%s] in task [%s]", key, factory.id);
                throw e;
            }

        }

        // Do the real-run
        return doRun();
    }

    /**
     * Order the inputs in topological order in order to evaluate them when
     * dependencies due to connections are satisfied
     */
    private ArrayList<String> getOrderedInputs() {
        // (1) Order the values to avoid dependencies
        // See http://en.wikipedia.org/wiki/Topological_sorting
        ArrayList<String> list = new ArrayList<String>();
        ArrayList<String> graph = new ArrayList<String>(values.keySet());

        Map<String, TreeSet<String>> forward_edges = new TreeMap<String, TreeSet<String>>();
        Map<String, TreeSet<String>> backwards_edges = new TreeMap<String, TreeSet<String>>();

        // Build the edge maps
        for (Entry<String, Value> entry : values.entrySet()) {
            final String to = entry.getKey();


            final Input input = entry.getValue().input;
            if (input.connections != null)
                for (Connection connection : input.connections)
                    for (Entry<String, DotName> pair : connection.getInputs()) {
                        String from = pair.getValue().get(0);
                        LOGGER.debug("[build] Adding edge from %s to %s", from, to);
                        addEdge(forward_edges, from, to);
                        addEdge(backwards_edges, to, from);
                    }
        }

        // Get the free nodes
        boolean done = false;
        while (!done) {
            done = true;
            Iterator<String> iterator = graph.iterator();
            while (iterator.hasNext()) {
                String n1 = iterator.next();
                final TreeSet<String> inSet = backwards_edges.get(n1);
                LOGGER.debug("Node %s has %d incoming edges", n1,
                        inSet == null ? 0 : inSet.size());
                if (inSet == null || inSet.isEmpty()) {
                    LOGGER.debug("Removing node %s", n1);
                    done = false;
                    list.add(n1);
                    iterator.remove();

                    // Remove the edges from n1
                    final TreeSet<String> nodes = forward_edges.get(n1);
                    if (nodes != null)
                        for (String n2 : nodes) {
                            LOGGER.debug("Removing edge from %s to %s", n1, n2);
                            backwards_edges.get(n2).remove(n1);
                        }
                }
            }
        }

        if (!graph.isEmpty())
            throw new ExperimaestroRuntimeException("Loop in the graph for task [%s]",
                    factory.id);

        return list;
    }

    static private void addEdge(Map<String, TreeSet<String>> edges,
                                String from, String to) {
        TreeSet<String> inSet = edges.get(from);
        if (inSet == null)
            edges.put(from, inSet = new TreeSet<String>());
        inSet.add(to);
    }

    /**
     * Set a parameter
     *
     * @param id    The identifier for this parameter (dot names)
     * @param value The value to be set (this should be an XML fragment)
     * @return True if the parameter was set and false otherwise
     */
    public final void setParameter(DotName id, Document value) {
        String name = id.get(0);

        if (!notNamedValues.isEmpty()) {
            for (String vname : notNamedValues) {
                try {
                    Value v = values.get(vname);
                    v.set(id, value);
                    return;
                } catch (NoSuchParameter e) {
                }
            }

        }

        Value inputValue = values.get(name);
        if (inputValue == null)
            throw new NoSuchParameter("Task %s has no input [%s]", factory.id,
                    name);

        inputValue.set(id.offset(1), value);

    }

    /**
     * Set a parameter.
     * <p/>
     * Utility function that wraps a string into an XML node
     *
     * @param id
     * @param value
     */
    public void setParameter(DotName id, String value) {
        final Document doc = wrapValue(value);
        setParameter(id, doc);
    }

    /**
     * Wrap a value into an XML document
     *
     * @param value
     * @return An XML document representing the value
     */
    static public Document wrapValue(String value) {
        final Document doc = XMLUtils.newDocument();
        Element element = doc
                .createElementNS(Manager.EXPERIMAESTRO_NS, "value");
        element.setAttributeNS("", "value", value);
        doc.appendChild(element);
        return doc;
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
     * @return A new Task
     */
    final public Task copy() {
        try {
            Constructor<? extends Task> constructor = this.getClass()
                    .getConstructor(new Class<?>[]{});
            Task copy = constructor.newInstance(new Object[]{});
            copy.init(this);
            return copy;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new ExperimaestroRuntimeException(t);
        }
    }

    /**
     * Initialise the Task from another one
     * <p/>
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

    /**
     * Run an experimental plan
     *
     * @param planString The plan string
     * @param singlePlan If the plan should be composed of only one plan
     * @throws ParseException
     */
    public List<Document> runPlan(String planString, boolean singlePlan) throws ParseException {
        PlanParser planParser = new PlanParser(new StringReader(planString));
        sf.net.experimaestro.plan.Node plans = planParser.plan();
        final Iterator<Map<String, String>> iterator = plans.iterator();

        ArrayList<Document> results = new ArrayList<>();

        LOGGER.info("Plan is %s", plans.toString());
        while (iterator.hasNext()) {
            Map<String, String> plan = iterator.next();
            if (singlePlan && iterator.hasNext()) {
                throw new ParseException("Plan has several parameter settings");
            }

            // Run a plan
            LOGGER.info("Running plan: %s",
                    Output.toString(" * ", plan.entrySet()));
            // First, get a copy of the task
            Task task = copy();

            // Set the parameters
            for (Map.Entry<String, String> kv : plan.entrySet())
                task.setParameter(DotName.parse(kv.getKey()), kv.getValue());

            // and run
            results.add(task.run());
        }

        return results;
    }
}
