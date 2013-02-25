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

import java.io.PrintStream;
import java.util.Iterator;

/**
 * Merge the output of several operators
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 21/2/13
 */
public class Union extends NAryOperator {

    @Override
    protected OperatorIterator _iterator() {

        return new OperatorIterator() {
            int parent = -1;
            Iterator<Value> iterator = null;

            @Override
            protected Value _computeNext() {
                while (parent < 0 || !iterator.hasNext()) {
                    if (++parent >= getParents().size())
                        return endOfData();
                    iterator = Union.this.getParent(parent).iterator();
                }
                return new Value(iterator.next().nodes);
            }
        };
    }

    @Override
    protected void printDOTNode(PrintStream out) {
        out.format("p%s [label=\"Union\"];%n", System.identityHashCode(this));
    }
}
