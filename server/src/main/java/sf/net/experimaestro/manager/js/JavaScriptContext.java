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
import sf.net.experimaestro.manager.scripting.LanguageContext;

/**
 * The JavaScript context when calling a function
 */
public class JavaScriptContext extends LanguageContext {
    private final Context context;
    private Scriptable scope;

    public JavaScriptContext(Context context, Scriptable scope) {
        this.context = context;
        this.scope = scope;
    }

    public Context context() {
        return context;
    }

    public Scriptable scope() {
        return scope;
    }
}
