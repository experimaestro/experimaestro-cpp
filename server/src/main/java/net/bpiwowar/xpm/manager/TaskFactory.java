package net.bpiwowar.xpm.manager;

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

import net.bpiwowar.xpm.exceptions.ExperimaestroCannotOverwrite;
import net.bpiwowar.xpm.exceptions.NoSuchParameter;
import net.bpiwowar.xpm.exceptions.ValueMismatchException;
import net.bpiwowar.xpm.exceptions.XPMScriptRuntimeException;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.manager.scripting.Deprecated;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.manager.scripting.Help;
import net.bpiwowar.xpm.manager.scripting.LanguageContext;
import net.bpiwowar.xpm.manager.scripting.Options;
import net.bpiwowar.xpm.scheduler.Commands;
import org.apache.commons.lang.NotImplementedException;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;


/**
 * Information about an experiment
 *
 * @author B. Piwowarski
 */
@Exposed
public abstract class TaskFactory {
    /**
     * The identifier of this experiment
     */
    protected QName id;

    /**
     * The version
     */
    String version;

    /**
     * The group
     */
    String group;

    /**
     * The module
     * <p>
     * Avoids serializing this
     */
    transient Module module;

    /**
     * The repository
     * <p>
     * Avoids serializing this
     */
    transient private Repository repository;

    /**
     * Initialise a task
     *
     * @param repository
     * @param id         The id of the task
     * @param version
     * @param group
     */
    public TaskFactory(Repository repository, QName id, String version,
                       String group) {
        this.repository = repository;
        this.id = id;
        this.version = version;
        this.group = group;
    }

    protected TaskFactory() {
    }

    protected TaskFactory(Repository repository) {
        this(repository, null, null, null);
    }

    public Repository getRepository() {
        return repository;
    }

    /**
     * Documentation in XHTML format
     */
    public String getDocumentation() {
        return format("<p>No documentation found for experiment %s</p>", id);
    }

    /**
     * Get the list of (potential) parameters
     *
     * @return a map of mappings from a qualified name to a named parameter or
     * null if non existent
     */
    abstract public Map<String, Input> getInputs();

    /**
     * Get the ouput of a task
     */
    abstract public Type getOutput();

    /**
     * Creates a new experiment
     */
    @Expose("create")
    public abstract Task create();

    /**
     * Returns the qualified name for this task
     */
    public QName getId() {
        return id;
    }

    public Object getVersion() {
        return version;
    }

    /**
     * Finish the initialisation of the factory
     */
    protected void init() {

    }

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        if (this.module == module)
            return;
        if (this.module != null)
            this.module.remove(this);
        this.module = module;
        module.addFactory(this);
    }

    @Help("Runs")
    @Expose(value = "run", context = true)
    public Json[] run(LanguageContext cx, @Options Map map, Parameters... parameters) throws ExperimaestroCannotOverwrite, ValueMismatchException, NoSuchParameter {
        final Task task = setParameters(cx, map);
        return new Json[] { task.run(parameters) };
    }

    @Help("Runs and asserts that there was a single output")
    @Expose(value = "run_", context = true)
    @Deprecated
    public Json run_(LanguageContext cx, @Options Map map, Parameters... parameters) throws ExperimaestroCannotOverwrite, ValueMismatchException, NoSuchParameter {
        return runOnce(cx, map, parameters);
    }

    @Help("Runs and asserts that there was a single output")
    @Expose(value = "evaluate", context = true)
    public Json runOnce(LanguageContext cx, @Options Map map, Parameters... parameters) throws ExperimaestroCannotOverwrite, ValueMismatchException, NoSuchParameter {
        final Json[] v = run(cx, map, parameters);
        if (v.length != 1) {
            throw new XPMScriptRuntimeException("Requested only one output, got %d", v.length);
        }
        return v[0];
    }

    @Expose(value = "simulate", context = true)
    public Object simulate(LanguageContext cx, @Options Map map, Parameters... parameters) throws Exception {
        final Task task = setParameters(cx, map);
        return task.run(true, parameters);
    }

    @Expose(context = true, value = "commands")
    public Commands commands(LanguageContext cx, JsonObject json, Parameters... parameters) throws ValueMismatchException, NoSuchParameter {
        if (parameters.length > 0) throw new NotImplementedException();
        Map<String, Object> map = new HashMap<>();
        json.entrySet().forEach(e -> map.put(e.getKey(), e.getValue()));
        final JsonTask configure = configure(cx, map);
        return configure.getCommands();
    }

    @Expose(context = true)
    public JsonTask configure(LanguageContext cx, @Options Map<String, Object> map, Parameters... parameters) throws ValueMismatchException, NoSuchParameter {
        final Task task = setParameters(cx, map);

        // Get parameters
        IdentityHashMap<Object, Parameters> pmap = new IdentityHashMap<>();
        Stream.of(parameters).forEach(p -> pmap.put(p.getKey(), p));
        final Commands commands = task.commands(pmap);

        return new JsonTask(task, commands);
    }

    protected Task setParameters(LanguageContext cx, @Options Map<String, Object> map) {
        // Set tasks input for validation
        final Task task = create();
        map.entrySet().stream().forEach(e -> {
            try {
                task.setParameter(DotName.parse(e.getKey()), cx.toJSON(e.getValue()));
            } catch (NoSuchParameter noSuchParameter) {
                throw new XPMScriptRuntimeException(noSuchParameter);
            }
        });
        return task;
    }
}
