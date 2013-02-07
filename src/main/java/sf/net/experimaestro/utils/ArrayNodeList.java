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

package sf.net.experimaestro.utils;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
* @author B. Piwowarski <benjamin@bpiwowar.net>
* @date 7/2/13
*/
class ArrayNodeList implements NodeList {

    final Node[] _nodes;
    private final Node[] nodes;

    public ArrayNodeList(Node[] nodes) {
        this.nodes = nodes;
        _nodes = nodes;
    }


    @Override
    public Node item(int index) {
        return _nodes[index];
    }

    @Override
    public int getLength() {
        return _nodes.length;
    }
}
