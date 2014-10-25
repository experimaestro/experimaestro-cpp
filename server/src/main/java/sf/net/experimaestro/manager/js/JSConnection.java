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
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import sf.net.experimaestro.exceptions.NoSuchParameter;
import sf.net.experimaestro.manager.Connection;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.utils.JSUtils;

import java.util.Arrays;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 2/4/13
 */
public class JSConnection extends Connection {
    private final Scriptable scope;
    private final Function function;
    private final String[] names;

    public JSConnection(DotName to, Scriptable scope, Function function, String names[]) {
        super(to);
        this.scope = scope;
        this.function = function;
        this.names = names;
    }

    @Override
    public Iterable<String> inputs() {
        return Arrays.asList(names);
    }

    @Override
    public Json computeValue(Task task) throws NoSuchParameter {
        Context context = Context.getCurrentContext();

        Object[] args = new Object[]{names.length};
        for (int i = 0; i < args.length; i++) {
            args[i] = new JSJson(task.getValue(new DotName(names[i])).get());
        }

        Object result = function.call(context, scope, null, args);
        return JSUtils.toJSON(scope, result);
    }
}
