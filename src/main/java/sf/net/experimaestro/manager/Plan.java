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

package sf.net.experimaestro.manager;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.exceptions.NoSuchParameter;
import sf.net.experimaestro.utils.CartesianProduct;
import sf.net.experimaestro.utils.SingleIterable;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * An experimental plan.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/2/13
 */
public class Plan {
    /**
     * Sub-plans
     */
    ArrayList<Plan> subplans = new ArrayList<>();


    /**
     * The task factory for this plan
     */
    TaskFactory factory;

    /**
     * Direct mappings
     */
    Iterable<? extends Mapping> mappings;

    public Plan(TaskFactory factory) {
        this.factory = factory;
    }

    public void setMappings(Iterable<? extends Mapping> mappings) {
        this.mappings = mappings;
    }

    // Connections

    /**
     * Run the plan
     *
     * @return An iterator over the generated documents
     */
    Iterator<Node> run() {
        // Prepares the iterator over the cartesian product
        final Iterator<Node[]> subiterator;

        if (subplans.size() == 0) {
            subiterator = ImmutableList.of(new Node[][] { new Node[0] }).iterator();
        } else {
            final Iterable<Document> iterables[] = new Iterable[subplans.size()];
            for (int i = 0; i < iterables.length; i++)
                iterables[i] = new SingleIterable(subplans.get(i).run());
            CartesianProduct<Node> subvalues = new CartesianProduct<>(Node.class, true, iterables);
            subiterator = subvalues.iterator();
        }

        // Returns the iterator
        return new AbstractIterator<Node>() {
            Iterator<? extends Mapping> iterator = null;
            Node[] subvalues = null;

            @Override
            protected Node computeNext() {
                if (iterator == null) {
                    iterator = mappings.iterator();
                    if (!iterator.hasNext())
                        return endOfData();
                }

                if (!iterator.hasNext())
                    subvalues = null;

                // Takes care of the sub-values
                if (subvalues == null) {
                    if (subiterator.hasNext())
                        subvalues = subiterator.next();
                    else
                        return endOfData();

                    iterator = mappings.iterator();
                }

                try {
                    final Task task = factory.create();
                    final Mapping mapping = iterator.next();
                    mapping.set(task);
                    return task.run();
                } catch (NoSuchParameter noSuchParameter) {
                    throw new ExperimaestroRuntimeException(noSuchParameter);
                }
            }
        };

    }
}
