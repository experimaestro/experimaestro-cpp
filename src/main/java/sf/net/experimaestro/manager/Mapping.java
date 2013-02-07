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

package sf.net.experimaestro.manager;

import com.google.common.collect.AbstractIterator;
import org.w3c.dom.Document;
import sf.net.experimaestro.exceptions.NoSuchParameter;
import sf.net.experimaestro.utils.CartesianProduct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * A mapping from a variable name to a value
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/2/13
 */
public interface Mapping {
    /**
     * Sets the value
     *
     * @param task
     */
    void set(Task task) throws NoSuchParameter;


    /**
     * Maps a name to several values
     */
    static public class Simple implements Iterable<Mapping> {
        DotName id;
        ArrayList<Object> values = new ArrayList<>();

        public Simple(DotName id, Object... values) {
            this.id = id;
            this.values.addAll(Arrays.asList(values));
        }

        public void add(Document value) {
            values.add(value);
        }

        @Override
        public Iterator<Mapping> iterator() {
            return new AbstractIterator<Mapping>() {
                Iterator<Object> iterator = values.iterator();


                protected Mapping computeNext() {
                    if (!iterator.hasNext())
                        return endOfData();
                    final Object next = iterator.next();

                    return new Mapping() {
                        @Override
                        public void set(Task task) throws NoSuchParameter {
                            if (next instanceof Document)
                                task.setParameter(id, (Document) next);
                            else
                                task.setParameter(id, (String) next);
                        }

                    };
                }
            };
        }
    }

    /**
     * Product of mappings
     */
    static public class Product implements Iterable<Mapping> {
        ArrayList<Iterable<Mapping>> list = new ArrayList<>();

        public void add(Iterable<Mapping> mappings) {
            list.add(mappings);
        }

        @Override
        public Iterator<Mapping> iterator() {
            final Iterator<Mapping[]> iterator = new CartesianProduct(Mapping.class, list.toArray(new Iterable[list.size()])).iterator();

            return new AbstractIterator<Mapping>() {
                @Override
                protected Mapping computeNext() {
                    if (!iterator.hasNext())
                        return endOfData();
                    final Mapping[] mappings = iterator.next();
                    return new Mapping() {
                        @Override
                        public void set(Task task) throws NoSuchParameter {
                            for (Mapping mapping : mappings)
                                mapping.set(task);

                        }
                    };
                }
            };
        }


    }
}

