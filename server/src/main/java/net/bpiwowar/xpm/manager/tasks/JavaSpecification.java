package net.bpiwowar.xpm.manager.tasks;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

/**
 * Specification for a java archive
 */
public class JavaSpecification {
    ArrayList<Path> jars;
    Map<String, JavaCommandSpecification> binaries;
}
