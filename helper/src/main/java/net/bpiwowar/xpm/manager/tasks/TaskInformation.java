package net.bpiwowar.xpm.manager.tasks;

import com.google.gson.JsonElement;
import net.bpiwowar.xpm.manager.TypeName;

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
    TypeName id;

    /**
     * The arguments that should be considered as paths
     */
    final ArrayList<PathArgument> pathArguments = new ArrayList<>();

    /**
     * Output type
     */
    TypeName output;

    /**
     * Inputs
     */
    Map<String, InputInformation> inputs = new HashMap<>();

    /**
     * Prefixes for namespaces - used for unique directory naming
     */
    Map<String, String> prefixes = new HashMap<>();

    /**
     * Constants to be added to the JSON
     */
    Map<String, JsonElement> constants = new HashMap<>();
}
