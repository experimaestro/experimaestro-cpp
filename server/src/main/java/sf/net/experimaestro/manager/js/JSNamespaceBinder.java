package sf.net.experimaestro.manager.js;

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

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.String2String;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/2/13
 */
public class JSNamespaceBinder implements String2String {
    private final Scriptable scope;

    public JSNamespaceBinder(Scriptable scope) {
        this.scope = scope;
    }

    @Override
    public String get(String id) {
        if (scope == null)
            return null;

        Object object = ScriptableObject.getProperty(scope, id);
        if (object == Scriptable.NOT_FOUND)
            return null;
        return JSUtils.toString(object);
    }
}
