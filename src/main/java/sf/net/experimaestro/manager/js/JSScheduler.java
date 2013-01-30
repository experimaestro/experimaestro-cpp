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

import com.sleepycat.je.DatabaseException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSConstructor;
import org.mozilla.javascript.annotations.JSFunction;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.log.Logger;

/**
 * Scheduler as seen by JavaScript
 *
 * @deprecated This is handled directly by XPM now
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSScheduler extends ScriptableObject {
    final static private Logger LOGGER = Logger.getLogger();

    private static final long serialVersionUID = 1L;

    public static final String CLASSNAME = "Scheduler";

    Scheduler scheduler;
    private XPMObject xpm;

    public JSScheduler() {
    }

    @JSConstructor
    public void jsConstructor(Scriptable scheduler, Scriptable xpm) {
        this.scheduler = (Scheduler) JSUtils.unwrap(scheduler);
        this.xpm = (XPMObject) JSUtils.unwrap(xpm);
    }

    @Override
    public String getClassName() {
        return CLASSNAME;
    }

    // ---- JavaScript functions ----

    /**
     * Run a command line experiment
     *
     * @param jsargs a native array
     * @return
     * @throws DatabaseException
     */
    @JSFunction("command_line_job")
    public void commandlineJob(String path, Object jsargs, Object jsoptions) throws Exception {
        xpm.getRootLogger().warn("scheduler.command_line_job is deprecated: use xpm.command_line_job");
        xpm.commandlineJob(path, jsargs, jsoptions);
    }

}

