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
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.utils.XMLUtils;

import javax.xml.xpath.XPathExpressionException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.lang.StrictMath.max;

/**
 * Grouping by
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 20/2/13
 */
public class GroupBy extends UnaryOperator {
    List<Operator> operators = new ArrayList<>();
    int[] indices;

    public void add(Operator operator) {
        operators.add(operator);
    }

    @Override
    protected void ensureConnections(Map<Operator, Operator> map) {
        Operator.ensureConnections(map, operators);
    }

    @Override
    protected Operator doCopy(boolean deep, Map<Object, Object> map) {
        GroupBy copy = new GroupBy();
        copy.operators = Lists.newArrayList(Operator.copy(operators, deep, map));
        return super.copy(deep, map, copy);
    }

    @Override
    protected void addNeededStreams(Collection<Operator> streams) {
        streams.addAll(operators);
    }

    @Override
    protected void doPostInit(List<Map<Operator, Integer>> parentStreams) throws XPathExpressionException {
        super.doPostInit(parentStreams);
        assert parentStreams.size() == 1;

        Map<Operator, Integer> map = parentStreams.get(0);

        indices = new int[operators.size()];
        int count = 0;
        for (Operator operator : operators) {
            indices[count++] = map.get(operator);
        }
    }

    @Override
    protected String getName() {
        if (indices != null)
            return String.format("GroupBy(%s)", Output.toString(", ", ArrayUtils.toObject(indices)));
        return "GroupBy";
    }

    @Override
    protected void printDOTNode(PrintStream out, Map<Operator, MutableInt> counts) {
        super.printDOTNode(out, counts);
        for (Operator operator : operators)
            out.format("p%s -> p%s [ style=\"dotted\", weight=0 ];%n",
                    System.identityHashCode(operator), System.identityHashCode(this));
    }

    @Override
    protected Iterator<ReturnValue> _iterator(final RunOptions runOptions) {
        int maxIndex = 0;
        for (int i : indices)
            maxIndex = max(maxIndex, i);
        final long positions[] = new long[maxIndex + 1];

        return new AbstractIterator<ReturnValue>() {
            PeekingIterator<Value> iterator = Iterators.peekingIterator(input.iterator(runOptions));

            @Override
            protected ReturnValue computeNext() {
                if (!iterator.hasNext())
                    return endOfData();


                Document document = XMLUtils.newDocument();
                Element array = document.createElementNS(Manager.EXPERIMAESTRO_NS, "array");
                document.appendChild(array);

                Value value = iterator.next();
                for (int i : indices)
                    positions[i] = value.context[i];

                ReturnValue rv = new ReturnValue(new DefaultContexts(value.context), document);
                add(array, value);

                main:
                while (iterator.hasNext()) {
                    value = iterator.peek();
                    for (int i : indices)
                        if (positions[i] != value.context[i]) {
                            break main;
                        }
                    iterator.next();
                    add(array, value);
                }

                return rv;

            }

            private void add(Element array, Value value) {
                Node node = value.nodes[0];
                assert value.nodes.length == 1;
                if (node instanceof Document)
                    node = (((Document) node).getDocumentElement());

                node = array.getOwnerDocument().adoptNode(node.cloneNode(true));
                array.appendChild(node);
            }
        };
    }
}
