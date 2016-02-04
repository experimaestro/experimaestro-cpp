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

import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.json.JsonBoolean;
import net.bpiwowar.xpm.utils.JsonAbstract;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * A parameter definition in a task factory / task
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@JsonAbstract
public abstract class Input {
    final static Logger LOGGER = Logger.getLogger();

    /**
     * Defines an optional parameter
     */
    boolean optional;

    /**
     * The type of the parameter
     */
    Type type;

    /**
     * Documentation for this parameter
     */
    String documentation;

    /**
     * Default value
     */
    Json defaultValue;

    /**
     * Used when this input is connected, i.e. its value is the
     * result of an XQuery expression based on other inputs
     */
    ArrayList<Connection> connections = new ArrayList<>();
    /**
     * Defines the namespace for wrapping values
     */
    private String namespace;
    /**
     * Whether the input should be copied into the output structure
     */
    private String copyTo;
    /**
     * The groups this input belongs to
     */
    private String[] groups;

    /**
     * Whether dependencies should be automatically processed for this object
     */
    private boolean nestedDependencies;

    /**
     * New input type
     *
     * @param type the type
     */
    public Input(Type type) {
        this.type = type;
    }

    /**
     * Returns whether the input is optional or not
     *
     * @return
     */
    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean nestedDependencies() {
        return nestedDependencies;
    }

    public void nestedDependencies(boolean nestedDependencies) {
        this.nestedDependencies = nestedDependencies;
    }

    /**
     * Get the documentation
     *
     * @return A string in XHTML
     */
    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public Type getType() {
        return type;
    }

    abstract Value newValue();

    public void printHTML(PrintWriter out) {
        out.println(documentation);
    }

    /** Set the default value for this input */
    public void setDefaultValue(Json defaultValue) {
        this.defaultValue = defaultValue.annotate(Constants.JSON_KEY_DEFAULT, JsonBoolean.TRUE).seal();
    }

    public void addConnection(Connection connection) {
        connections.add(connection);
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getCopyTo() {
        return copyTo;
    }

    public void setCopyTo(String copyTo) {
        this.copyTo = copyTo;
    }

    public void setGroups(String[] groups) {
        this.groups = groups;
    }

    public boolean inGroup(String groupId) {
        if (groups == null) {
            return false;
        }
        for (String group : groups) {
            if (groupId.equals(group)) {
                return true;
            }
        }
        return false;
    }

    public Json getDefault() {
        return defaultValue;
    }
}
