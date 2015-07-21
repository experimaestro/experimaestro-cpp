package sf.net.experimaestro.manager.plans;

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

import bpiwowar.argparser.utils.Output;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import sf.net.experimaestro.exceptions.NoSuchParameter;
import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.manager.scripting.ScriptContext;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * An operator associated to a task
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
public class TaskOperator extends UnaryOperator {
    /**
     * The associated plan
     */
    private Plan plan;
    private Map<DotName, Integer> mappings;

    /**
     * Construct a new task node
     *
     * @param plan The associated plan
     */
    public TaskOperator(ScriptContext sc, Plan plan) {
        super(sc);
        this.plan = plan;
    }

    public Plan getPlan() {
        return plan;
    }

    @Override
    protected Operator doCopy(boolean deep, Map<Object, Object> map) {
        TaskOperator copy = new TaskOperator(ScriptContext.get(), plan);
        copy.mappings = new TreeMap<>(mappings);
        return super.copy(deep, map, copy);
    }

    @Override
    boolean cacheIterator() {
        // We want values to be cached
        return true;
    }

    /**
     * Creates an iterator
     *
     */
    @Override
    protected Iterator<ReturnValue> _iterator() {
        return new AbstractIterator<ReturnValue>() {
            // Parent values
            final Iterator<Value> iterator = input != null ?
                    input.iterator(scriptContext) : ImmutableList.of(new Value()).iterator();

            @Override
            protected ReturnValue computeNext() {
                if (!iterator.hasNext())
                    return endOfData();

                Value value = iterator.next();

                Task task = plan.createTask();
                for (Map.Entry<DotName, Integer> entry : mappings.entrySet()) {
                    try {
                        task.setParameter(entry.getKey(), value.nodes[entry.getValue()]);
                    } catch (NoSuchParameter noSuchParameter) {
                        throw new XPMRuntimeException(noSuchParameter);
                    }
                }

                try {
                    scriptContext.setTaskOperator(TaskOperator.this);
                    final Json result = task.run(scriptContext);
                    return new ReturnValue(new DefaultContexts(value.context), result);
                } catch (NoSuchParameter | ValueMismatchException e) {
                    throw new XPMRuntimeException(e);
                } catch (XPMRuntimeException e) {
                    e.addContext("While running task %s", plan.getFactory().getId());
                    throw e;
                }
            }
        };

    }


    @Override
    public boolean equals(Object obj) {
        // either the same plan node, or the same plan
        if (obj instanceof TaskOperator) {
            TaskOperator other = (TaskOperator) obj;
            return other == this || getPlan().equals(other.getPlan());
        }

        return super.equals(obj);
    }


    public Operator getInput() {
        return input;
    }


    @Override
    protected String getName() {
        return String.format("task %s(%s)", plan.getFactory().getId(), Output.toString(", ", mappings.entrySet(),
                o -> {
                    return String.format("%s/%d", o.getKey(), o.getValue());
                }));
    }

    public void setMappings(Map<DotName, Integer> mappings) {
        this.mappings = mappings;
    }
}
