package sf.net.experimaestro.manager.java;

import net.bpiwowar.experimaestro.tasks.Runner;
import org.apache.commons.lang.SystemUtils;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.manager.json.JsonString;
import sf.net.experimaestro.scheduler.Command;
import sf.net.experimaestro.scheduler.Commands;

import java.util.Map;

/**
 *
 */
public class JavaCommandLauncher implements JavaCommandBuilder {
    private final JavaCommandSpecification specification;

    public JavaCommandLauncher(Map<String, JavaCommandSpecification> binaries) {
        if (SystemUtils.IS_OS_UNIX || SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC_OSX) {
            specification = binaries.get("unix");
        } else {
            throw new RuntimeException("Unsupported OS");
        }
    }

    @Override
    public Commands build(String taskClassname, JsonObject json) {
        final Command command = new Command();

        final Commands commands = new Commands(command);

        // Add command
        command.add(specification.path);

        // TaskReference class name
        command.add(taskClassname);

        // Working directory
        command.add(Command.WorkingDirectory.INSTANCE);

        // Parameter file
        command.add(new Command.JsonParameterFile("json", json));

        return commands;
    }

    @Override
    public void setEnvironment(JsonObject json, Map<String, String> environment) {
        StringBuilder builder = new StringBuilder();

        // Sets JVM options
        final Json jvm = json.get(JavaTaskFactory.JVM_OPTIONS);
        if (jvm != null && jvm instanceof JsonObject) {
            final Json memory = ((JsonObject) jvm).get("memory");
            if (memory instanceof JsonString) {
                final Object s = memory.get();
                builder.append("-Xmx");
                builder.append(s);
                builder.append(' ');
            }
        }

        environment.put(specification.jvm_options, builder.toString());
    }
}
