package net.bpiwowar.xpm.manager.tasks;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

/**
 * Specific informations about a java task
 */
@ClassChooserInstance(name = "python")
public class ExternalTasksInformation extends TasksInformation {
    Map<Path, Path> tasks_file;
    String[] version = new String[2];
}
