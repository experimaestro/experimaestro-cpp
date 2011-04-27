package sf.net.experimaestro.plan;

import java.util.Iterator;
import java.util.Map;

import sf.net.experimaestro.utils.EmptyIterator;

abstract public class Node implements Iterable<Map<String, String>> {
	static final public Node EMPTY = new Node() {
		@Override
		public Iterator<Map<String, String>> iterator() {
			return EmptyIterator.create();
		}
	};
}
