package sf.net.experimaestro.tasks;

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
