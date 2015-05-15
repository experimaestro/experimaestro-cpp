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

import sf.net.experimaestro.connectors.Launcher;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.exceptions.ExitException;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.scheduler.TokenResource;
import sf.net.experimaestro.scheduler.Transaction;

/**
 * General functions available to all scripting languages
 */
@Exposed
public class Functions {
    @Expose()
    @Help("Retrieve (or creates) a token resource with a given xpath")
    static public TokenResource token_resource(
            @Argument(name = "path", help = "The path of the resource") String path
    ) throws ExperimaestroCannotOverwrite {
        return Transaction.evaluate((em, t) -> {
            final Resource resource = Resource.getByLocator(em, path);
            final TokenResource tokenResource;
            if (resource == null) {
                tokenResource = new TokenResource(path, 0);
                tokenResource.save(t);
            } else {
                if (!(resource instanceof TokenResource))
                    throw new AssertionError(String.format("Resource %s exists and is not a token", path));
                tokenResource = (TokenResource) resource;
            }

            return tokenResource;
        });
    }

    @Expose(optional = 1)
    @Help("Defines a new relationship between a network share and a path on a connector")
    public void define_share(@Argument(name = "host", help = "The logical host")
                             String host,
                             @Argument(name = "share")
                             String share,
                             @Argument(name = "connector")
                             SingleHostConnector connector,
                             @Argument(name = "path")
                             String path,
                             @Argument(name = "priority")
                             Integer priority) {
        Scheduler.defineShare(host, share, connector, path, priority == null ? 0 : priority);
    }

    @Expose(optional = 2)
    public void exit(@Argument(name = "code", help = "The exit code") int code,
                     @Argument(name = "message", help = "Formatting template") String message,
                     @Argument(name = "objects", help = "Formatting arguments") Object... objects) {
        if (message == null) throw new ExitException(code);
        if (objects == null) throw new ExitException(code, message);
        throw new ExitException(code, message, objects);
    }

    @Expose
    @Help("Defines the default launcher")
    public void set_default_launcher(Launcher launcher) {
        ScriptContext.threadContext().setDefaultLauncher(launcher);
    }
}
