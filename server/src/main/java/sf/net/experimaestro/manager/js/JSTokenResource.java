/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import sf.net.experimaestro.scheduler.TokenResource;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 26/11/12
 */
public class JSTokenResource extends ScriptableObject implements Wrapper {

    private TokenResource resource;

    public JSTokenResource() {
    }

    @Override
    public String getClassName() {
        return "TokenResource";
    }

    public void jsConstructor(Object resource) {
        this.resource = (TokenResource) resource;
    }


    public void jsFunction_set_limit(int limit) {
        resource.setLimit(limit);
    }

    @Override
    public TokenResource unwrap() {
        return resource;
    }
}
