package net.bpiwowar.xpm.manager;

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

import net.bpiwowar.xpm.exceptions.NoSuchParameter;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.json.JsonArray;
import net.bpiwowar.xpm.manager.scripting.ScriptContext;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 23/1/13
 */
public class ArrayValue extends Value {
    /**
     * The input nodes
     */
    JsonArray array;

    public ArrayValue(ArrayInput input) {
        this.input = input;
    }

    @Override
    public Value getValue() throws NoSuchParameter {
        return this;
    }

    @Override
    public void set(Json value) {
        this.array = (JsonArray) value;
    }

    @Override
    public void process(ScriptContext taskContext) {
    }

    @Override
    public Json get() {
        return array;
    }
}
