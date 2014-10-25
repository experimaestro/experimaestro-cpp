package sf.net.experimaestro.manager;

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

import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.utils.log.Logger;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * A parameter definition in a task factory / task
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
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
     * Unnamed option
     */
    boolean unnamed;
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

    public void setDefaultValue(Json defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void addConnection(Connection connection) {
        connections.add(connection);
    }

    public boolean isUnnamed() {
        return unnamed;
    }

    public void setUnnamed(boolean unnamed) {
        this.unnamed = unnamed;
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
        for (String group : groups) {
            if (groupId.equals(group)) {
                return true;
            }
        }
        return false;
    }
}
