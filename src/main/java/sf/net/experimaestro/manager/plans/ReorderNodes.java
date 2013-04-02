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
import org.apache.commons.lang.ArrayUtils;
import sf.net.experimaestro.manager.json.Json;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 12/3/13
 */
public class ReorderNodes extends UnaryOperator {
    int mapping[];

    /**
     * Gives the mapping between input and output
     * The index of the array is the output, the stored value is the input
     *
     * @param mapping
     */
    public ReorderNodes(int[] mapping) {
        this.mapping = mapping;
    }

    @Override
    protected Operator doCopy(boolean deep, Map<Object, Object> map) {
        return new ReorderNodes(Arrays.copyOf(mapping, mapping.length));
    }

    @Override
    protected String getName() {
        return String.format("reorder [%s]", Output.toString(", ", ArrayUtils.toObject(mapping)));
    }

    @Override
    protected Iterator<ReturnValue> _iterator(final RunOptions runOptions) {
        return new AbstractIterator<ReturnValue>() {
            Iterator<Value> inputIterator = input.iterator(runOptions);

            @Override
            protected ReturnValue computeNext() {
                if (!inputIterator.hasNext())
                    return endOfData();

                Value value = inputIterator.next();
                Json[] nodes = new Json[mapping.length];
                for (int i = 0; i < mapping.length; i++)
                    nodes[i] = value.nodes[mapping[i]];
                return new ReturnValue(new DefaultContexts(value.context), nodes);
            }
        };
    }
}
