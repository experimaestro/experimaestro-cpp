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
import org.mozilla.javascript.*;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.locks.LockType;
import sf.net.experimaestro.scheduler.*;
import sf.net.experimaestro.utils.log.Logger;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

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
     * @return
     * @throws DatabaseException
     */
    public void jsFunction_addCommandLineJob(String identifier, Object jsargs, Object jsoptions) throws DatabaseException {
        // --- XPMProcess arguments: convert the javascript array into a Java array
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

        NativeObject options = jsoptions instanceof Undefined ? null : (NativeObject) jsoptions;

        // --- Create the task
        final Connector connector;

        if (options != null && options.has("connector", options)) {
            connector = ((JSConnector)options.get("connector", options)).getConnector();
        } else
            connector = new LocalhostConnector();

        // Store connector in database
        scheduler.put(connector);

        CommandLineTask task = new CommandLineTask(scheduler, connector, identifier, args);


        // --- Options

        if (!(jsoptions instanceof Undefined)) {
            // --- XPMProcess launcher
            if (options != null) {
                if (options.has("launcher", options)) {
                    final Object launcher = options.get("launcher", options);
                    if (launcher != null && !(launcher instanceof UniqueTag))
                        task.setLauncher(((JSLauncher) launcher).getLauncher());
                }
            }


            // --- Resources to lock
            if (options.has("lock", options)) {
                NativeArray resources = (NativeArray) options.get("launcher", options);
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

            // --- Redirect standard output
            final Object stdout = options.get("stdout", options);

            // --- Redirect standard error
            final Object stderr = options.get("stderr", options);
        }


        // --- Add it
        scheduler.add(task);
    }

}