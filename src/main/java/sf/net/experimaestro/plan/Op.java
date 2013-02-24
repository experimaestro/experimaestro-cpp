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

package sf.net.experimaestro.plan;

import com.google.common.collect.AbstractIterator;
import sf.net.experimaestro.utils.CartesianProduct;
import sf.net.experimaestro.utils.IteratorSequence;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class Op extends Node {
	Node n1, n2;
	OpType type;

	Op(OpType type, Node n1, Node n2) {
		this.type = type;
		this.n1 = n1;
		this.n2 = n2;
	}

	public String toString() {
		return String.format("(%s %s %s)", n1, type, n2);
	}

	@Override
	public Iterator<Map<String, Value>> iterator() {
		switch (type) {
		case OR: {
			@SuppressWarnings("unchecked")
			IteratorSequence<Map<String, Value>> a = new IteratorSequence<>(
					n1.iterator(), n2.iterator());
			return a;
		}
		
		case MULT: {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			final CartesianProduct<Map<String, Value>> p = new CartesianProduct(
					Map.class, n1, n2);
			return new AbstractIterator<Map<String, Value>>() {
				Iterator<Map<String, Value>[]> iterator = p.iterator();

				@Override
				protected Map<String, Value> computeNext() {
					if (iterator.hasNext()) {
						Map<String, Value>[] values = iterator.next();
                        Map<String, Value> map = new TreeMap<>();
						for (Map<String, Value> v : values) {
							map.putAll(v);
						}
						return map;
					}

					return endOfData();
				}
			};

		}
		}

		return null;
	}
}
