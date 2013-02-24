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

import java.util.AbstractList;
import java.util.List;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 20/2/13
 */
abstract public class UnaryOperator extends Operator {
    /**
     * The input operator
     */
    Operator input;

    @Override
    public List<Operator> getParents() {
        return new AbstractList<Operator>() {
            @Override
            public Operator get(int index) {
                if (index > size())
                    throw new IndexOutOfBoundsException();
                return input;
            }

            @Override
            public Operator set(int index, Operator element) {
                if (index > size())
                    throw new IndexOutOfBoundsException();
                final Operator old = input;
                input = element;
                return old;
            }

            @Override
            public int size() {
                return input == null ? 0 : 1;
            }
        };
    }

    @Override
    public void addParent(Operator parent) {
        if (this.input != null)
            throw new IndexOutOfBoundsException("Trying to add more than one parent to a TaskNode");
        super.addParent(parent);
        input = parent;

    }


}
