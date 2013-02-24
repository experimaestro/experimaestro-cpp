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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A constant in a plan just generate nodes
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 21/2/13
 */
public class Constant extends Operator {
    List<Node> nodes  = new ArrayList<>();

    public Constant(Node... documents) {
        nodes.addAll(Arrays.asList(documents));
    }

    @Override
    public List<Operator> getParents() {
        return ImmutableList.of();
    }

    @Override
    protected OperatorIterator _iterator() {
        return new OperatorIterator() {
            Iterator<? extends Node> iterator = nodes.iterator();

            @Override
            protected Value _computeNext() {
                return iterator.hasNext() ?
                        new Value(iterator.next()) : endOfData();
            }
        };
    }

    @Override
    protected Operator doInit(Multimap<Operator, Operator> streams) throws XPathExpressionException {
        return this;
    }

    @Override
    protected Operator doPrepare(StreamRequest request) throws XPathExpressionException {
        return this;
    }

    @Override
    public void addParent(Operator parent) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void printDOTNode(PrintStream out) {
        out.format("p%s [label=\"XML (%d)\"];%n", System.identityHashCode(this), nodes.size());
    }

    public void add(Constant source) {
        nodes.addAll(source.nodes);
    }
}
