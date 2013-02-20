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

import com.google.common.collect.AbstractIterator;
import org.w3c.dom.Node;

import java.util.Iterator;
import java.util.List;

/**
 * Cartesian product of inputs
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 20/2/13
 */
public class Product extends Merge {


    @Override
    public List<Plan> plans() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected OperatorIterator _iterator() {

        final Iterator<Value>[] inputs = new Iterator[parents.size()];
        for (int i = 0; i < parents.size(); i++)
            inputs[i] = parents.get(i).iterator();


        return new OperatorIterator() {
            boolean first = true;
            Value current = null;

            @Override
            protected Value _computeNext() {
                if (first) {
                    Node[] nodes = new Node[inputs.length];
                    for (int i = 0; i < parents.size(); i++) {
                        if (!inputs[i].hasNext())
                            return endOfData();
                        else {
                            set(nodes, i);
                        }
                    }
                    first = false;
                    return current = new Value(nodes);
                }

                Node[] nodes = new Node[inputs.length];
                for (int i = 0; i < parents.size(); i++) {
                    if (inputs[i].hasNext()) {
                        set(nodes, i);

                        for (int j = 0; --j >= 0; ) {
                            inputs[j] = parents.get(j).iterator();
                            set(nodes, j);
                        }

                        System.arraycopy(current.nodes, i + 1, nodes, i + 1, parents.size() - i - 1);

                        return new Value(nodes);
                    }
                }

                return endOfData();
            }

            private void set(Node[] nodes, int i) {
                final Value value = inputs[i].next();
                assert value.nodes.length == 1;
                nodes[i] = value.nodes[0];
            }
        };
    }


}
