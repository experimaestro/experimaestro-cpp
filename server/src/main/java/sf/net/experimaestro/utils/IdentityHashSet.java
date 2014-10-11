package sf.net.experimaestro.utils;

import java.util.AbstractSet;
import java.util.IdentityHashMap;
import java.util.Iterator;

/**
 * A hash set where the hash is computed directly on the object
 */
public class IdentityHashSet<T> extends AbstractSet<T> {
    IdentityHashMap<T, T> map = new IdentityHashMap<>();

    @Override
    public Iterator<T> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean add(T t) {
        return map.put(t, t) == null;
    }

    @Override
    public int size() {
        return map.size();
    }
}
