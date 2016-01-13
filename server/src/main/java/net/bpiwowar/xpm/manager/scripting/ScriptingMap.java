package net.bpiwowar.xpm.manager.scripting;

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

import net.bpiwowar.xpm.utils.log.Logger;

import java.nio.file.Path;
import java.util.Map;

/**
 * A JavaScript wrapper for {@linkplain Path}
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
public class ScriptingMap extends WrapperObject<Map> {
    final static Logger LOGGER = Logger.getLogger();

    public ScriptingMap(Map object) {
        super(object);
    }

    @Expose(mode = ExposeMode.FIELDS)
    public Object get(String value) {
        return object.get(value);
    }

    @Expose(mode = ExposeMode.FIELDS)
    public void put(Object key, Object value) {
        object.put(key, value);
    }

}
