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

import net.bpiwowar.xpm.manager.*;
import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.*;

import java.util.Map;

/**
 * TaskReference factory created from java reflection
 */
@Exposed
public class JavaTaskFactory extends ExternalTaskFactory {

    transient JavaCommandBuilder javaCommandBuilder;

    public static final String JVM_OPTIONS = "$jvm";

    private String taskClassname;

    /**
     * Used only for deserialization
     */
    protected JavaTaskFactory() {
    }

    @Override
    protected Commands build(Task task) {
        return javaCommandBuilder.build(taskClassname, task);
    }

    /**
     * Initialise a task
     *
     * @param javaCommandBuilder   The class path
     * @param repository  The repository
     * @param information Java task information
     */
    public JavaTaskFactory(JavaCommandBuilder javaCommandBuilder, Repository repository, JavaTaskInformation information) {
        super(repository, information);
        this.javaCommandBuilder = javaCommandBuilder;
        taskClassname = information.taskClassname;

        // Adds JVM specific arguments
        JsonInput input = new JsonInput(new Type(Constants.XP_OBJECT));
        input.setOptional(true);
        this.inputs.put(JVM_OPTIONS, input);
    }

    public void setJavaCommandBuilder(JavaCommandBuilder builder) {
        this.javaCommandBuilder = builder;
    }

    @Override
    public void setEnvironment(JsonObject json, Map<String, String> environment) {
        javaCommandBuilder.setEnvironment(json, environment);
    }
}
