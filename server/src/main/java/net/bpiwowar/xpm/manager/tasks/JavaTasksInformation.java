package net.bpiwowar.xpm.manager.tasks;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

/**
 * Specific informations about a java task
 */
@ClassChooserInstance(name = "java")
public class JavaTasksInformation extends TasksInformation {
    ArrayList<Path> jars;
    Map<String, JavaCommandSpecification> binaries;
}
