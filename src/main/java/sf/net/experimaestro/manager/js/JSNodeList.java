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

package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sf.net.experimaestro.utils.RangeUtils;
import sf.net.experimaestro.utils.XMLUtils;

import java.util.Iterator;

import static com.google.common.collect.Ranges.closed;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/3/13
 */
public class JSNodeList extends JSBaseObject implements Iterable<Node> {
    private final NodeList list;

    public JSNodeList(NodeList list) {
        this.list = list;
    }

    @Override
    public Object[] getIds() {
        return RangeUtils.toIntegerArray(closed(0, list.getLength()));
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return index >= 0 && index < list.getLength();
    }

    @Override
    public Object get(int index, Scriptable start) {
        return new JSNode(list.item(index));
    }

    @Override
    public Iterator<Node> iterator() {
        return (Iterator<Node>) XMLUtils.iterable(list).iterator();
    }
}
