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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Join / cartesian product of inputs
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 20/2/13
 */
public class Product extends NAryOperator {
    /**
     * A context reference
     */
    static public class JoinReference {
        int streamIndex;
        int contextIndex;

        public JoinReference(int streamIndex, int contextIndex) {
            this.streamIndex = streamIndex;
            this.contextIndex = contextIndex;
        }
    }

    static public class Join {
        long current;
        List<JoinReference> references = new ArrayList<>();

        public void add(JoinReference reference) {
            references.add(reference);
        }
    }

    /**
     * List of joins
     */
    List<Join> joins = new ArrayList<>();

    public void addJoin(Join join) {
        joins.add(join);
    }

    @Override
    protected OperatorIterator _iterator() {

        final Iterator<Value>[] inputs = new Iterator[parents.size()];
        for (int i = 0; i < parents.size(); i++)
            inputs[i] = parents.get(i).iterator();


        return new OperatorIterator() {
            boolean first = true;
            Node current [] = new Node[inputs.length];

            @Override
            protected Value _computeNext() {
                // First loop
                Node[] nodes = new Node[inputs.length];

                if (first) {
                    for (int i = 0; i < parents.size(); i++) {
                        if (!inputs[i].hasNext())
                            return endOfData();
                        else {
                            if (i > 0)
                                next(current, i);
                        }
                    }
                    first = false;
                }

                for (int i = 0; i < parents.size(); i++) {
                    if (inputs[i].hasNext()) {
                        next(current, i);

                        for (int j = i; --j >= 0; ) {
                            inputs[j] = parents.get(j).iterator();
                            next(current, j);
                        }

                        System.arraycopy(current, 0, nodes, 0, inputs.length);

                        return new Value(nodes);
                    }
                }

                return endOfData();
            }

            private void next(Node[] nodes, int i) {
                final Value value = inputs[i].next();
                assert value.nodes.length == 1;
                nodes[i] = value.nodes[0];
            }
        };
    }

    @Override
    protected void printDOTNode(PrintStream out) {
        out.format("p%s [label=\"Product/Join\"];%n", System.identityHashCode(this));
    }
}
