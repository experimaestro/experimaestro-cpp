package sf.net.experimaestro.utils.arrays;

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

import java.util.AbstractList;
import java.util.List;

import static java.lang.String.format;

/**
 * An immutable view over lists
 * Assumes that lists will not be changed while using this view
 */
public class ListUnionView<E> extends AbstractList<E> {
    List<E>[] lists;
    int[] offsets;

    public ListUnionView(List<E>... lists) {
        this.lists = lists;
        this.offsets = new int[lists.length];
        int offset = 0;
        for (int i = 0; i < lists.length; i++) {
            offset = lists[i].size() + offset;
            offsets[i] = offset;
        }
    }

    @Override
    public E get(int index) {
        for (int i = 0; i < offsets.length; i++) {
            if (offsets[i] > index) {
                return lists[i].get(index - offsets[i]);
            }
        }
        throw new IndexOutOfBoundsException(format("Index %d out of bounds (%d)", index, size()));
    }


    @Override
    public int size() {
        return offsets[offsets.length - 1];
    }
}
