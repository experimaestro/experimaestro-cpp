/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2011 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package sf.net.experimaestro.manager.js;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.*;

import sf.net.experimaestro.locks.LockType;
import sf.net.experimaestro.scheduler.CommandLineTask;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

import com.sleepycat.je.DatabaseException;

/**
 * Scheduler as seen by JavaScript
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSScheduler extends ScriptableObject {
    final static private Logger LOGGER = Logger.getLogger();

    private static final long serialVersionUID = 1L;

    public static final String CLASSNAME = "Scheduler";

    Scheduler scheduler;

    public JSScheduler() {
    }

    public void jsConstructor(Scriptable scheduler) {
        if (scheduler != null) {
            LOGGER.info(scheduler.toString());
            this.scheduler = (Scheduler) ((NativeJavaObject) scheduler)
                    .unwrap();
        }
    }

    @Override
    public String getClassName() {
        return CLASSNAME;
    }

    // ---- JavaScript functions ----

    /**
     * Run a command line experiment
     *
     * @param jsargs      a native array
     * @param jsresources E4X object
     * @return
     * @throws DatabaseException
     */
    public void jsFunction_addCommandLineJob(String identifier, Object jsargs,
                                             Object jsresources, Object jsrunner) throws DatabaseException {
        // --- Process arguments: convert the javascript array into a Java array
        // of String
        LOGGER.debug("Adding command line job");
        final String[] args;
        if (jsargs instanceof NativeArray) {
            NativeArray array = ((NativeArray) jsargs);
            List<String> list = new ArrayList<String>();
            XPMObject.flattenArray(array, list);
            args = new String[list.size()];
            list.toArray(args);
        } else
            throw new RuntimeException(format(
                    "Cannot handle an array of type %s", jsargs.getClass()));

        CommandLineTask task = new CommandLineTask(scheduler, identifier, args);

        // --- Resources
        if (!(jsresources instanceof Undefined)) {
            NativeArray resources = ((NativeArray) jsresources);
            for (int i = (int) resources.getLength(); --i >= 0; ) {
                NativeArray array = (NativeArray) resources.get(i, resources);
                assert array.getLength() == 2;
                Resource resource = scheduler.getResource(XPMObject.toString(array
                        .get(0, array)));
                LockType lockType = LockType.valueOf(XPMObject.toString(array.get(
                        1, array)));
                LOGGER.debug("Adding dependency on [%s] of tyep [%s]", resource,
                        lockType);
                task.addDependency(resource, lockType);
            }
        }

        // --- Add it
        scheduler.add(task);
    }

}