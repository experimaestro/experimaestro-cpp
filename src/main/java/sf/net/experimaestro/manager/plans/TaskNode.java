/*
 * This file is part of experimaestro.
 * Copyright (c) 2013 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.manager.plans;

import bpiwowar.argparser.utils.Output;
import com.google.common.collect.ImmutableList;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.exceptions.NoSuchParameter;
import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Task;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A plan node that can be iterated over
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TaskNode extends UnaryOperator {
    /**
     * The associated plan
     */
    private Plan plan;
    private ArrayList<DotName> mappings;

    public Plan getPlan() {
        return plan;
    }

    /**
     * Construct a new task node
     *
     * @param plan The associated plan
     */
    public TaskNode(Plan plan) {
        this.plan = plan;
    }

    /**
     * Creates an iterator
     */
    @Override
    protected OperatorIterator _iterator() {
        return new OperatorIterator() {
            Value parentValue;

            // Parent values
            final Iterator<Value> iterator = input != null ?
                    input.iterator() : ImmutableList.of(new Value(new Node[0])).iterator();

            @Override
            protected Value _computeNext() {
                if (!iterator.hasNext())
                    return endOfData();

                Value value = iterator.next();

                Task task = plan.createTask();
                assert mappings.size() == value.nodes.length;
                for(int i = 0; i < value.nodes.length; i++) {
                    try {
                        task.setParameter(mappings.get(i), value.nodes[i]);
                    } catch (NoSuchParameter noSuchParameter) {
                        throw new AssertionError();
                    }
                }
                try {
                    return new Value(task.run());
                } catch (NoSuchParameter | ValueMismatchException e) {
                    throw new ExperimaestroRuntimeException(e);
                }
            }
        };

    }


    @Override
    public boolean equals(Object obj) {
        // either the same plan node, or the same plan
        if (obj instanceof TaskNode) {
            TaskNode other = (TaskNode) obj;
            return other == this || getPlan().equals(other.getPlan());
        }

        return super.equals(obj);
    }


    public Operator getInput() {
        return input;
    }

    @Override
    protected void printDOTNode(PrintStream out) {
        out.format("p%s [label=\"task %s(%s)\"];%n", System.identityHashCode(this), plan.getFactory().getId(),
                Output.toString(", ", mappings));
    }


    public void setMappings(ArrayList<DotName> mappings) {
        this.mappings = mappings;
    }
}
