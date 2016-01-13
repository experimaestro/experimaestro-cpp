package sf.net.experimaestro.tasks;

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

import bpiwowar.argparser.OrderedArgument;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

/**
 * Created by bpiwowar on 1/10/14.
 */
@TaskDescription(name = "get-tasks", project = "xpmanager")
public class GetTasks extends AbstractTask {
    final static Logger LOGGER = Logger.getLogger();

    @OrderedArgument
    String classpath;

    @Override
    public int execute() throws Throwable {
        // Convert File to a URL
        String[] files = classpath.split(":");
        URL[] urls = new URL[files.length];

        // Put first experimaestro, so that we can use easily the annotation classes
        for (int i = 0; i < files.length; i++) {
            urls[i] = new File(files[i]).toURL();
        }

        // Create a new class loader with the directory
        ClassLoader cl = new URLClassLoader(urls);

        // Get the implementing class
        ArrayList<Class<?>> list = new ArrayList<>();// JavaTasksIntrospection.getTaskClasses(cl);

        for (Class<?> aClass : list) {
            System.err.println("--- " + aClass);
        }

        return 0;
    }

}
