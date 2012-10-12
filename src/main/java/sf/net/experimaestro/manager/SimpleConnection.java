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

import java.util.Arrays;
import java.util.Map;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 12/10/12
 */
public class SimpleConnection extends Connection {
    final DotName from;
    final String path;

    /**
     * Simple connection between two inputs
     * @param from Dot name of the input
     * @param path Path
     * @param to DotName
     */
    public SimpleConnection(DotName from, String path, DotName to) {
        super(to);
        this.from = from;
        this.path = path;
   }


    @Override
    public Iterable<? extends Map.Entry<String,DotName>> getInputs() {
        return Arrays.asList(Pair.create("x", from));
    }

    @Override
    public String getXQuery() {
        return "$x/" + path;
    }

    @Override
    public String toString() {
        return String.format("SimpleConnection[%s - %s -> %s]", from, path, to);
    }
}
