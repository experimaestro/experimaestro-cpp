package sf.net.experimaestro.plan;

import java.util.Iterator;
import java.util.Map;

import sf.net.experimaestro.utils.CartesianProduct;
import sf.net.experimaestro.utils.IteratorSequence;
import sf.net.experimaestro.utils.iterators.AbstractIterator;
import bpiwowar.argparser.GenericHelper;

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
	public Iterator<Map<String, String>> iterator() {
		switch (type) {
		case OR: {
			@SuppressWarnings("unchecked")
			IteratorSequence<Map<String, String>> a = new IteratorSequence<Map<String, String>>(
					n1.iterator(), n2.iterator());
			return a;
		}
		
		case MULT: {
			@SuppressWarnings("unchecked")
			final CartesianProduct<Map<String, String>> p = new CartesianProduct(
					Map.class, true, n1, n2);
			return new AbstractIterator<Map<String, String>>() {
				Iterator<Map<String, String>[]> iterator = p.iterator();

				@Override
				protected boolean storeNext() {
					if (iterator.hasNext()) {
						Map<String, String>[] values = iterator.next();
						value = GenericHelper.newTreeMap();
						for (Map<String, String> v : values) {
							value.putAll(v);
						}
						return true;
					}
					return false;
				}
			};

		}
		}

		return null;
	}
}
