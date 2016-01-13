package sf.net.experimaestro.manager.plans;

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

import sf.net.experimaestro.manager.json.Json;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 20/2/13
 */
public class Value {
    /**
     * The value ID
     */
    long id;

    /**
     * The next value (useful when sharing)
     */
    Value next = null;

    /**
     * The produced node
     */
    Json[] nodes;

    /**
     * The context
     */
    long context[];

    public Value(Json... nodes) {
        this.nodes = nodes;
    }

    public Value(long[] context, Json... nodes) {
        this.context = context;
        this.nodes = nodes;
    }

    public Json[] getNodes() {
        return nodes;
    }
}
