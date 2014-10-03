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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import sf.net.experimaestro.utils.WrappedResult;

import java.util.*;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 5/3/13
 */
public class Order<T> implements Iterable<Set<T>> {
    List<Set<T>> list = new ArrayList<>();

    public Order(List<Set<T>> list) {
        this.list = list;
    }

    public Order() {
    }

    /**
     * Add an operator to the last set to be sorted (and create a new one if asked)
     *
     * @param t      The element
     * @param newSet Whether to create a new set
     */
    public void add(T t, boolean newSet) {
        if (list.isEmpty() || newSet)
            list.add(new HashSet<T>());
        list.get(list.size() - 1).add(t);
    }

    /**
     * Get a a full order compatible with this one
     */
    public Iterable<T> items() {
        Set[] inputs = list.toArray(new Set[list.size()]);
        return Iterables.concat(inputs);
    }

    /**
     * Combine two or more orders
     * <p/>
     * The result is the more specific order that is less specific than than both more specific orders
     * than those combined, i.e. it is the greater element that
     * <ul>
     * <li>has a meet with the two given orders</li>
     * <li>The meet with each given order is the lowest one</li>
     * </ul>
     *
     * @param orders The orders to combine
     * @return <tt>true</tt> if the orders were compatible, <tt>false</tt> otherwise
     */
    static public <T> WrappedResult<Order<T>> combine(final Order<T>... orders) {
        List<Set<T>> newList = new ArrayList<>();

        Iterator<Set<T>> iterators[] = new Iterator[orders.length];
        Set sets[] = new Set[orders.length];
        for (int i = 0; i < orders.length; i++) {
            sets[i] = ImmutableSet.of();
            iterators[i] = orders[i].list.iterator();
        }

        int remaining = sets.length;
        while ((remaining = advance(iterators, sets, remaining)) > 0) {
            if (remaining == 1) {
                newList.add(sets[0]);
                sets[0] = ImmutableSet.of();
            } else {
                Set<T> intersection = intersection(sets, remaining);
                if (intersection.isEmpty()) {
                    return new WrappedResult(false, new Order(newList));
                }

                // Removed ordered items
                for (int i = 0; i < remaining; i++)
                    sets[i].removeAll(intersection);

                newList.add(intersection);

            }
        }

        return new WrappedResult(true, new Order(newList));
    }


    private static <T> Set<T> intersection(Set<T>[] sets, int remaining) {
        Set<T> intersection = new HashSet<>();
        main:
        for (T t : sets[0]) {
            for (int i = 1; i < remaining; i++) {
                if (!sets[i].contains(t))
                    continue main;
            }
            intersection.add(t);
        }
        return intersection;
    }

    /**
     * Advance one of the iterators
     */
    static private <T> int advance(Iterator<Set<T>>[] iterators, Set[] sets, int remaining) {
        int count = 0;
        for (int i = 0; i < remaining; i++) {
            while (sets[i].isEmpty() && iterators[i].hasNext())
                sets[i] = new HashSet<>(iterators[i].next());

            if (!sets[i].isEmpty()) {
                count++;
            } else {
                // Swap with the last
                if (i != remaining - 1) {
                    sets[i] = sets[remaining - 1];
                    iterators[i] = iterators[remaining - 1];
                }
                i--;
                remaining--;
            }
        }
        return count;
    }


    /**
     * Remove the specified operator from the list
     *
     * @param operator
     * @return
     */
    public void remove(Operator operator) {
        for (int i = list.size(); --i >= 0; ) {
            Set<T> operators = list.get(i);
            if (operators.remove(operator)) {
                if (operators.isEmpty())
                    list.remove(i);
                break;
            }
        }
    }


    public boolean combine(Order<T> other) {
        WrappedResult<Order<T>> result = Order.combine(this, other);
        list = result.get().list;
        return result.isSuccess();
    }


    @Override
    public Iterator<Set<T>> iterator() {
        return list.iterator();
    }

    public int size() {
        int size = 0;
        for (Set<T> operators : list) {
            size += operators.size();
        }

        return size;
    }


    public void flatten() {
        List<Set<T>> newList = new ArrayList<>();
        for (T t : items()) {
            ImmutableSet.of(t);
            Set<T> set = new ObjectArraySet<>(1);
            set.add(t);
            newList.add(set);
        }
        list = newList;
    }
}
