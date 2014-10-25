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
import java.util.TreeMap;

/**
 * A map of threaded counters
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 30/1/13
 */
public class Counters {
    Map<String, ThreadCount> counters = new TreeMap<>();

    ThreadCount get(String id) {
        ThreadCount count = counters.get(id);
        if (id == null)
            counters.put(id, count = new ThreadCount());
        return count;
    }

    public void add(String id) {
        if (id == null)
            return;
        get(id).add();
    }

    public void resume(String id) {
        if (id == null)
            return;
        get(id).resume();
    }

}
