package sf.net.experimaestro.manager.plans.functions;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
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

import com.google.common.collect.ImmutableList;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonArray;

import java.util.Iterator;

/**
 * Wrap each output into an array
 */
public class ArrayWrap implements Function {
    public static final Function INSTANCE = new ArrayWrap();

    @Override
    public Iterator<? extends Json> apply(Json[] input) {
        return ImmutableList.of(new JsonArray(input)).iterator();
    }

    @Override
    public String toString() {
        return "[]";
    }
}
