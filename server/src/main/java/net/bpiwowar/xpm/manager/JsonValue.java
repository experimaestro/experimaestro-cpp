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

import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.scripting.ScriptContext;
import net.bpiwowar.xpm.utils.log.Logger;

/**
 * A simple XML value
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JsonValue extends Value {
    final static private Logger LOGGER = Logger.getLogger();

    private Json value;

    public JsonValue(Input input) {
        super(input);
    }

    public JsonValue() {
    }

    @Override
    public void process(ScriptContext taskContext) {
        // If there is no value, takes the default
        if (value == null && input.defaultValue != null) {
            LOGGER.debug("Setting default value [%s]", input.defaultValue);
            value = input.defaultValue.seal();
        }
    }

    @Override
    public void set(Json value) {
        LOGGER.debug("Value set to [%s]", value);
        this.value = value;
    }

    @Override
    public Json get() {
        return value;
    }

    @Override
    protected void init(Value _other) {
        JsonValue other = (JsonValue) _other;
        super.init(other);
        if (other.value != null)
            value = other.value.seal();
    }

    @Override
    public boolean isSet() {
        return value != null;
    }

    @Override
    public Json getAsInput() {
        return super.getAsInput();
    }
}
