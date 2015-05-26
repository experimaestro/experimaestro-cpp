package sf.net.experimaestro.manager.scripting;

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

import org.mozilla.javascript.NativeFunction;
import sf.net.experimaestro.manager.js.JavaScriptContext;
import sf.net.experimaestro.utils.log.Logger;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * A JavaScript wrapper for {@linkplain Path}
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
public class ScriptingList extends WrapperObject<List> {
    final static Logger LOGGER = Logger.getLogger();

    public ScriptingList(List object) {
        super(object);
    }

    @Expose(mode = ExposeMode.PROPERTY, value = "length")
    int length() {
        return object.size();
    }

    @Expose(mode = ExposeMode.INDEX)
    public Object get(int index) {
        return object.get(index);
    }


    @Expose(context = true)
    public void sort(JavaScriptContext jcx, NativeFunction f) {
        LOGGER.debug("Sorting list");
        Collections.sort(object, (a, b) -> {
            final double result = (Double)f.call(jcx.context(), jcx.scope(), null, new Object[]{a, b});
            if (result > 0) return 1;
            if (result < 0) return -1;
            return 0;
        });
    }
}
