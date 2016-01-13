package net.bpiwowar.xpm.manager.tasks;

import net.bpiwowar.xpm.manager.QName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TaskInformation {
    /**
     * Task id
     */
    QName id;

    /**
     * The arguments that should be considered as paths
     */
    final ArrayList<PathArgument> pathArguments = new ArrayList<>();

    /**
     * Output type
     */
    QName output;

    /**
     * Inputs
     */
    Map<String, InputInformation> inputs = new HashMap<>();

    /**
     * Prefixes for namespaces - used for unique directory naming
     */
    Map<String, String> prefixes = new HashMap<>();
}
