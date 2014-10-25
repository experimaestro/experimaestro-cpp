package sf.net.experimaestro.manager.js.object;

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

import org.mozilla.javascript.Wrapper;
import sf.net.experimaestro.manager.js.JSBaseObject;
import sf.net.experimaestro.manager.js.JSObjectDescription;
import sf.net.experimaestro.scheduler.Command;

/**
 * Just a pipe
 */
@JSObjectDescription(name = "Pipe")
public class Pipe extends JSBaseObject implements Wrapper {
    @Override
    public Object unwrap() {
        return Command.Pipe.getInstance();
    }

    @Override
    public String toString() {
        return "PIPE";
    }
}
