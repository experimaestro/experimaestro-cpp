package sf.net.experimaestro.utils;

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

import java.util.Map;
import java.util.Map.Entry;

public class Maps {

    /**
     * Convert the values of a set
     *
     * @param <Key>
     * @param <Value1>
     * @param <Value2>
     * @param map1
     * @param map2
     * @param converter
     */
    public static final <Key, Value1, Value2> Map<Key, Value2> convert(Map<Key, Value1> map1,
                                                                       Map<Key, Value2> map2, Converter<Value1, Value2> converter) {
        for (Entry<Key, Value1> entry : map1.entrySet())
            map2.put(entry.getKey(), converter.convert(entry.getValue()));
        return map2;
    }
}
