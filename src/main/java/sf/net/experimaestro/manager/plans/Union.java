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

import javax.xml.xpath.XPathExpressionException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Merge the output of several operators
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 21/2/13
 */
public class Union extends NAryOperator {

    @Override
    protected Operator doCopy(boolean deep, Map<Object, Object> map) {
        Union copy = new Union();
        return super.copy(deep, map, copy);
    }

    @Override
    protected Iterator<ReturnValue> _iterator(final RunOptions runOptions) {

        return new AbstractIterator<ReturnValue>() {
            int parent = -1;
            Iterator<Value> iterator = null;

            @Override
            protected ReturnValue computeNext() {
                while (parent < 0 || !iterator.hasNext()) {
                    if (++parent >= getParents().size())
                        return endOfData();
                    iterator = Union.this.getParent(parent).iterator(runOptions);
                }

                Value value = iterator.next();
                ReturnValue rv = new ReturnValue(new UnionContexts(parent, value.context), value.nodes);
                return rv;
            }

        };
    }

    static private class UnionContexts implements Contexts {
        private final int parent;
        private final long[] context;

        public UnionContexts(int parent, long[] context) {
            this.parent = parent;
            this.context = context;
        }

        @Override
        public long get(int stream, int index) {
            if (stream != this.parent)
                return -1;
            return context[index];
        }
    }


    @Override
    protected String getName() {
        return "Union";
    }

    @Override
    protected void doPostInit(List<Map<Operator, Integer>> parentStreams) throws XPathExpressionException {
    }
}
