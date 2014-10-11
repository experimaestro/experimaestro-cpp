package sf.net.experimaestro.utils.arrays;

import java.util.AbstractList;
import java.util.List;

import static java.lang.String.format;

/**
 * An immutable view over lists
 * Assumes that lists will not be changed while using this view
 */
public class ListUnionView<E> extends AbstractList<E> {
    List<E> [] lists;
    int[] offsets;

    public ListUnionView(List<E>... lists) {
        this.lists = lists;
        this.offsets = new int[lists.length];
        int offset = 0;
        for(int i = 0; i < lists.length; i++) {
            offset = lists[i].size() + offset;
            offsets[i] = offset;
        }
    }

    @Override
    public E get(int index) {
        for(int i = 0; i < offsets.length; i++) {
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
