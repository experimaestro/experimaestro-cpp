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
import sf.net.experimaestro.connectors.Launcher;
import sf.net.experimaestro.connectors.OARLauncher;
import sf.net.experimaestro.utils.log.Logger;

/**
 * OAR launcher.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSOARLauncher extends ScriptableObject implements JSLauncher {
    public static final String CLASSNAME = "OARLauncher";
    final static private Logger LOGGER = Logger.getLogger();
    private static final long serialVersionUID = 1L;
    OARLauncher launcher;

    public JSOARLauncher() {
    }

    public void jsConstructor() {
        this.launcher = new OARLauncher();
    }

    @Override
    public String getClassName() {
        return CLASSNAME;
    }


    @Override
    public Launcher getLauncher() {
        return launcher;
    }

    // ---- JavaScript functions ----

}