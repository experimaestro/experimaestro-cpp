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

import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.exceptions.NoSuchParameter;
import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.manager.Task;

import javax.xml.xpath.XPathExpressionException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * A plan node that can be iterated over
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TaskNode extends SimpleOperator {
    /**
     * Add a reference to ourselves in produced values
     */
    protected boolean addSelf;


    /**
     * The associated plan
     */
    private Plan plan;

    /**
     * Our mappings
     */
    Mappings mappings;

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

    @Override
    public List<Plan> plans() {
        return Arrays.asList(plan);
    }

    /**
     * Creates an iterator
     */
    @Override
    protected OperatorIterator _iterator() {
        return new OperatorIterator() {
            Value parentValue;

            Iterator<? extends Mapping> iterator;

            // Parent values
            final Iterator<Value> parentIterator = input != null ? input.iterator() : null;

            @Override
            protected Value _computeNext() {
                // We started, so we remove the reference
                currentIterator = null;

                // Get the next value from which we can iterate
                while (iterator == null || !iterator.hasNext()) {
                    if (input != null) {
                        if (!parentIterator.hasNext())
                            return endOfData();
                        parentValue = parentIterator.next();
                    } else if (iterator != null)
                        return endOfData();

                    iterator = mappings.iterator(parentValue);
                }

                // Execute
                try {
                    final Mapping mapping = iterator.next();
                    final Task task = plan.createTask();
                    mapping.set(task);
                    final Node node = task.run();


                    return new Value(new Node[]{node});
                } catch (NoSuchParameter | ValueMismatchException | XPathExpressionException e) {
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


    @Override
    protected TaskNode init(HashSet<Operator> processed) throws XPathExpressionException {
        if (super.init(processed) != null) {
            // Init the mappings
            mappings = plan.init(this);
            return this;
        }
        return null;
    }


    public Operator getInput() {
        return input;
    }
}
