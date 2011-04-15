package sf.net.experimaestro.utils.arrays;

import java.util.AbstractList;
import java.util.List;

public class ListInvertAdaptator<T> extends AbstractList<T> {
	private List<T> list;

	public ListInvertAdaptator(List<T> list) {
		this.list = list;
	}
	
	@Override
	public T get(int index) {
		return list.get(size() - index - 1);
	}

	@Override
	public int size() {
		return list.size();
	}

}
