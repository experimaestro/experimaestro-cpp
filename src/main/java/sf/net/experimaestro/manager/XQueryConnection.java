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

package sf.net.experimaestro.manager;

import sf.net.experimaestro.utils.Pair;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 12/10/12
 */
public class XQueryConnection extends Connection {
    ArrayList<Pair<String, DotName>> inputs = new ArrayList<>();
    String query;

    public XQueryConnection(DotName to, String query) {
        super(to);
        this.query = query;
    }


    @Override
    public Iterable<? extends Map.Entry<String, DotName>> getInputs() {
        return inputs;
    }

    @Override
    public String getXQuery() {
        return query;
    }

    public void bind(String var, DotName name) {
        inputs.add(Pair.create(var, name));
    }
}
