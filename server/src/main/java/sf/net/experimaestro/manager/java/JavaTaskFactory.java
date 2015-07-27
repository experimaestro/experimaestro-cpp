package sf.net.experimaestro.manager.java;

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

import net.bpiwowar.experimaestro.tasks.Runner;
import sf.net.experimaestro.connectors.NetworkShare;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.*;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.manager.json.JsonString;
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.scheduler.Command;
import sf.net.experimaestro.scheduler.Commands;
import sf.net.experimaestro.scheduler.Dependency;
import sf.net.experimaestro.scheduler.Resource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * TaskReference factory created from java reflection
 */
@Exposed
public class JavaTaskFactory extends TaskFactory {


    ArrayList<PathArgument> pathArguments;

    Map<String, String> prefixes;
    transient java.nio.file.Path[] classpath;

    public static final String JVM_OPTIONS = "$jvm";

    private final Map<String, Input> inputs = new HashMap<>();

    private String taskClassname;
    private Type output;

    /**
     * Used only for deserialization
     */
    protected JavaTaskFactory() {
    }

    /**
     * Initialise a task
     *
     * @param classpath   The class path
     * @param repository  The repository
     * @param information Java task information
     */
    public JavaTaskFactory(java.nio.file.Path[] classpath, Repository repository, JavaTaskInformation information) {
        super(repository);
        this.classpath = classpath;
//         = new JavaTaskInformation(classInfo, namespaces);
        this.id = information.id;
        taskClassname = information.taskClassname;
        this.prefixes = information.prefixes;
        this.pathArguments = information.pathArguments;
        this.output = new ValueType(information.output);


        // Add inputs
        for (Map.Entry<String, InputInformation> entry : information.inputs.entrySet()) {
            String name = entry.getKey();
            final InputInformation field = entry.getValue();
            Input input = new JsonInput(getType(field));
            input.setDocumentation(field.help);
            input.setOptional(!field.required);
            inputs.put(name, input);
        }


        // Adds JVM
        JsonInput input = new JsonInput(new Type(Constants.XP_OBJECT));
        input.setOptional(true);
        this.inputs.put(JVM_OPTIONS, input);
    }

    private Type getType(InputInformation field) {
        return new ValueType(field.getValueType());
    }

    @Override
    public Map<String, Input> getInputs() {
        return inputs;
    }

    @Override
    public Type getOutput() {
        return output;
    }

    @Override
    public Task create() {
        final JavaTask task = new JavaTask(this);
        task.init();
        return task;
    }

    @Override
    public Commands commands(JsonObject json, boolean simulate) {
        final Command command = new Command();

        Command classpath = new Command();
        final Commands commands = new Commands(command);

        Arrays.asList(this.classpath).stream().forEach(f -> {
            classpath.add(new Command.Path(f));
            classpath.add(new Command.String(":"));
        });

        command.add("java", "-cp");
        command.add(classpath);

        // Sets JVM options
        final Json jvm = json.get(JVM_OPTIONS);
        if (jvm != null && jvm instanceof JsonObject) {
            final Json memory = ((JsonObject) jvm).get("memory");
            if (memory instanceof JsonString) {
                final Object s = memory.get();
                command.add("-Xmx" + s);
            }
        }

        // Runner class name
        command.add(Runner.class.getName());

        // TaskReference class name
        command.add(taskClassname);

        // Working directory
        command.add(Command.WorkingDirectory.INSTANCE);

        // Parameter file
        command.add(new Command.JsonParameterFile("json", json));

        // Check dependencies
        if (!simulate) {
            for (Json element : json.values()) {
                if (element instanceof JsonObject) {
                    JsonObject object = (JsonObject) element;
                    final Json r = object.get(Constants.XP_RESOURCE.toString());
                    if (r == null) continue;

                    final Object o = r.get();
                    Resource resource;
                    if (o instanceof Resource) {
                        resource = (Resource) o;
                    } else {
                        try {
                            resource = Resource.getByLocator(NetworkShare.uriToPath(o.toString()));
                        } catch (SQLException e) {
                            throw new XPMRuntimeException(e, "Error while searching the resource %s the task %s depends upon",
                                    o.toString(), getId());
                        }
                        if (resource == null) {
                            throw new XPMRuntimeException("Cannot find the resource %s the task %s depends upon",
                                    o.toString(), getId());
                        }
                    }
                    final Dependency lock = resource.createDependency("READ");
                    commands.addDependency(lock);
                }
            }
        }

        return commands;
    }

    public void setClasspath(java.nio.file.Path[] classpath) {
        this.classpath = classpath;
    }
}
