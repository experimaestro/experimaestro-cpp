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

import org.apache.commons.vfs2.FileSystemException;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.annotations.JSFunction;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.utils.JSUtils;

/**
 * A resource
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 26/11/12
 */
public class JSResource extends ScriptableObject implements Wrapper {

    private Resource resource;

    public JSResource() {}

    @Override
    public String getClassName() {
        return "Resource";
    }

    public void jsConstructor(Object resource) throws FileSystemException {
        this.resource = (Resource) JSUtils.unwrap(resource);


    }


    @Override
    public Resource unwrap() {
        return resource;
    }

    @Override
    @JSFunction("toString")
    public String toString() {
        return resource == null ? "[null]" : resource.toString();
    }

}
