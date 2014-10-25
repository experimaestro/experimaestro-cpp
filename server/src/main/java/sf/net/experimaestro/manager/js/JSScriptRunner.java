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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.ScriptRunner;
import sf.net.experimaestro.utils.JSUtils;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 18/1/13
 */
public class JSScriptRunner implements ScriptRunner {
    private Scriptable scope;

    public JSScriptRunner(Scriptable scope) {
        this.scope = scope;
    }


    @Override
    public Object evaluate(String script) throws Exception {
        final Object result = Context.getCurrentContext().evaluateString(scope, script, "inline", 1, null);
        if (JSUtils.isXML(result))
            return JSUtils.toDocument(null, result, new QName(Manager.EXPERIMAESTRO_NS, "parameters"));
        return JSUtils.toString(result);
    }
}
