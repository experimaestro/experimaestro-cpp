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

import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.TokenResource;
import sf.net.experimaestro.scheduler.Transaction;

/**
 * A token resource wrapper
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSTokenResource extends JSResource {

    private TokenResource resource;

    @JSFunction
    public JSTokenResource(Resource resource) {
        super(resource);
        this.resource = (TokenResource) resource;
    }

    @Override
    public String getClassName() {
        return "TokenResource";
    }


    @JSFunction("set_limit")
    public void setLimit(int limit) {
        if (resource.getId() == null) {
            resource.setLimit(limit);
        }
        // Get a database copy of this resource first
        Transaction.run(em -> {
            resource = em.find(TokenResource.class, resource.getId());
            resource.setLimit(limit);
        });
    }

    @Override
    public TokenResource unwrap() {
        return resource;
    }
}
