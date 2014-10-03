/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.utils;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * A trie of sequences
 *
 * Not a high efficiency implementation, but very generic.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 23/11/12
 */
public class Trie<C extends Comparable, O extends List<? extends C>> {
    TreeMap<C, Trie<C,O>> children;

    /**
     * Put a new sequence into the trie
     * @param sequence The sequence to insert
     * @return true if a new node was inserted in the trie
     */
    public boolean put(O sequence) {
        return put(sequence.iterator());
    }

    /**
     * Inserts a new element in the subtrie
     * @param iterator
     * @return
     */
    private boolean put(Iterator<? extends C> iterator) {
        if (!iterator.hasNext())
            return false;

        final C key = iterator.next();
        Comparable x = key;
        Trie<C, O> child = null;
        if (children == null) {
            children = new TreeMap<>();
        } else {
            child = children.get(key);
        }

        // If a child exists, insert
        if (child != null)
            return child.put(iterator);

        // Insert a new element
        child = new Trie();
        child.put(iterator);
        children.put(key, child);
        return true;
    }


    /**
     * Find the node of the trie that matches a given prefix
     * @param sequence The prefix to match
     * @return A trie node or null if not found
     */
    public Trie<C,O> find(List<? extends C> sequence) {
        return find(sequence.iterator());
    }

    private Trie<C, O> find(Iterator<? extends C> iterator) {
        if (!iterator.hasNext())
            return this;

        if (children == null)
            return null;

        final C next = iterator.next();
        final Trie<C, O> subTrie = children.get(next);
        if (subTrie == null)
            return null;

        return subTrie.find(iterator);
    }


    public Iterable<C> childrenKeys() {
        if (children == null)
            return new EmptyIterable<>();
        return children.keySet();
    }
}
