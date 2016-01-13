package net.bpiwowar.xpm.utils;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
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

import com.google.common.collect.AbstractIterator;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A Map backed up by a stack of maps
 * <p/>
 * New values are inserted on the top of the stack
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 26/2/13
 */
public class SetStack<Value> extends AbstractSet<Value> {
    SetStack<Value> previous;
    Set<Value> set = new HashSet<>();

    public SetStack(SetStack<Value> previous) {
        this.previous = previous;
    }

    public SetStack() {
    }

    @Override
    public Iterator<Value> iterator() {
        return new AbstractIterator<Value>() {
            Iterator<Value> iterator = set.iterator();
            SetStack<Value> current = SetStack.this;

            @Override
            protected Value computeNext() {
                while (!iterator.hasNext()) {
                    if (current.previous == null)
                        return endOfData();
                    current = current.previous;
                    iterator = current.set.iterator();
                }
                return iterator.next();
            }
        };
    }

    @Override
    public int size() {
        int size = 0;
        for (SetStack<Value> current = this; current != null; current = current.previous) {
            size += current.set.size();
        }
        return size;
    }

    @Override
    public boolean contains(Object o) {
        for (SetStack<Value> current = this; current != null; current = current.previous)
            if (current.set.contains(o))
                return true;
        return false;
    }

    @Override
    public boolean add(Value value) {
        for (SetStack<Value> current = this; current != null; current = current.previous)
            if (current.set.contains(value))
                return false;
        return set.add(value);
    }


    public SetStack<Value> newSet() {
        return new SetStack<>(this);
    }
}
