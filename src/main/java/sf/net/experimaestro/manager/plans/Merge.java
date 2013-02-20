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

import java.util.ArrayList;
import java.util.List;

/**
 * An operator with multiple inputs
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 20/2/13
 */
abstract public class Merge extends Operator {
    ArrayList<Operator> parents;

    @Override
    public List<Operator> getParents() {
        return parents;
    }

    @Override
    public void addParent(Operator parent) {
        parents.add(parent);
        parent.children.add(this);
    }
}
