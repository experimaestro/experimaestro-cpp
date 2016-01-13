package net.bpiwowar.xpm.manager.tasks;

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

import net.bpiwowar.xpm.connectors.NetworkShare;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.*;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * TaskReference factory created from java reflection
 */
@Exposed
public class JavaTaskFactory extends TaskFactory {
    ArrayList<PathArgument> pathArguments;

    Map<String, String> prefixes;
    transient JavaCommandBuilder javaCommandBuilder;

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
     * @param javaCommandBuilder   The class path
     * @param repository  The repository
     * @param information Java task information
     */
    public JavaTaskFactory(JavaCommandBuilder javaCommandBuilder, Repository repository, JavaTaskInformation information) {
        super(repository);
        this.javaCommandBuilder = javaCommandBuilder;
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


        // Adds JVM specific arguments
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
        Commands commands = javaCommandBuilder.build(taskClassname, json);


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
                    final Dependency lock = resource.createDependency((DependencyParameters)null);
                    commands.addDependency(lock);
                }
            }
        }

        return commands;
    }

    public void setJavaCommandBuilder(JavaCommandBuilder builder) {
        this.javaCommandBuilder = builder;
    }

    public void setEnvironment(JsonObject json, Map<String, String> environment) {
        javaCommandBuilder.setEnvironment(json, environment);
    }
}
