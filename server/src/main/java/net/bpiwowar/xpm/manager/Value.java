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

import net.bpiwowar.xpm.exceptions.NoSuchParameter;
import net.bpiwowar.xpm.exceptions.ValueMismatchException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.scripting.ScriptContext;
import net.bpiwowar.xpm.utils.log.Logger;

import java.lang.reflect.Constructor;

/**
 * Represents a value that can be set
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public abstract class Value {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The corresponding input
     */
    Input input;

    /**
     * Used to copy the value
     */
    protected Value() {
    }

    /**
     * Construct a new value
     *
     * @param input
     */
    public Value(Input input) {
        this.input = input;
    }

    final public Type getType() {
        return input.getType();
    }

    /**
     * Returns the value object corresponding to this path
     *
     * @param id The ID
     * @return The value
     */
    public abstract Value getValue(DotName id) throws NoSuchParameter;


    /**
     * Set to the given value
     *
     * @param value
     */
    public abstract void set(Json value);

    /**
     * Process the value before it can be accessed by a task to run
     *
     * @param taskContext
     */
    public abstract void process(ScriptContext taskContext) throws NoSuchParameter, ValueMismatchException;

    /**
     * Get the value
     * <p/>
     * This method is called by a {@link Task} after {@link #process(ScriptContext)}.
     *
     * @return A valid XML document or null if not set
     */
    public abstract Json get();

    /**
     * This method is called once by a {@link Task} after {@link #process(ScriptContext)}.
     *
     * @param task
     */
    void processConnections(Task task) throws NoSuchParameter {
        LOGGER.debug("Processing %d connections for [%s]", input.connections.size(), task.factory.getId());
        // Do not process if we do not have connections...
        for (Connection connection : input.connections) {
            LOGGER.debug("Processing connection [%s]", connection);
            Value destination = task.getValue(connection.to);
            Json json = connection.computeValue(task);
            destination.set(json);
        }
    }


    final public Value copy() {
        try {
            Constructor<? extends Value> constructor = this.getClass()
                    .getConstructor();
            Value copy = constructor.newInstance();
            copy.init(this);
            return copy;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new XPMRuntimeException(t);
        }
    }

    protected void init(Value other) {
        this.input = other.input;
    }

    /**
     * Checks whether the value was set
     */
    public boolean isSet() {
        return get() != null;
    }


    public Input getInput() {
        return input;
    }

    /**
     * Computes the JSON that will be used as an external task input
     * @return
     */
    public Json getAsInput() {
        return get();
    }
}
