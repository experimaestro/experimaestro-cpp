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

import org.mozilla.javascript.Wrapper;
import sf.net.experimaestro.scheduler.Resource;

/**
 * A resource
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 26/11/12
 */
public class JSResource extends JSBaseObject implements Wrapper {

    private Resource resource;

    @JSFunction
    public JSResource(Resource resource) {
        this.resource = resource;
    }

    @Override
    public String getClassName() {
        return "Resource";
    }


    @Override
    @JSFunction("toString")
    public String toString() {
        return resource == null ? "[null]" : ("[Resource " + resource.getLocator().toString() + "]");
    }

    @JSFunction
    public JSDependency lock(String lockType) {
        return new JSDependency(resource.createDependency(lockType));
    }


    @Override
    public Object unwrap() {
        return resource;
    }
}
