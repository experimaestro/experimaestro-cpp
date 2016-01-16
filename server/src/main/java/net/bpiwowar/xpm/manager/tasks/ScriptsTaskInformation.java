package net.bpiwowar.xpm.manager.tasks;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Specific informations about a java task
 */
@ClassChooserInstance(name = "script")
public class ScriptsTaskInformation extends TasksInformation {
    /** The task files*/
    Map<Path, Path> tasks_file;

    /** The command */
    List<CommandArgument> command;

    /** Namespace prefixes */
    Map<String, String> namespaces;
}
