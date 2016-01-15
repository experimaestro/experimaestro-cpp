package net.bpiwowar.xpm.commands;

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

import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.Dependency;
import net.bpiwowar.xpm.utils.JsonAbstract;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * An abstract command
 */
@Exposed
@JsonAbstract
public abstract class AbstractCommand implements Iterable<AbstractCommand> {
    /**
     * List of dependencies attached to this command
     * <p>
     * The dependencies are not saved during serialization since this will be handled
     * by the resource
     */
    protected transient ArrayList<Dependency> dependencies = new ArrayList<>();

    /**
     * The input redirect
     * <p>
     * Null indicates that the input should be the null device
     */
    Redirect inputRedirect = Redirect.INHERIT;

    /**
     * The output stream redirect.
     * <p>
     * Null indicates that the output should be discarded
     */
    Redirect outputRedirect = null;

    /**
     * The error stream redirect.
     * <p>
     * Null indicates that the output should be discarded
     */
    Redirect errorRedirect = Redirect.INHERIT;

    /**
     * Standard input
     */
    CommandOutput standardInput;

    /**
     * Process each dependency contained in a command or subcommand
     * @param consumer The consumer to be fed
     */
    final public void forEachDependency(Consumer<Dependency> consumer) {
        dependencies().forEach(consumer::accept);
    }

    void prepare(CommandContext env) {
        if (standardInput != null) {
            try {
                standardInput.prepare(env);
                inputRedirect = Redirect.from(standardInput.getFile(env));
            } catch (IOException e) {
                throw new XPMRuntimeException(e);
            }
        }
    }

    protected AbstractCommand() {}

    public Redirect getOutputRedirect() {
        return outputRedirect;
    }

    public Redirect getErrorRedirect() {
        return errorRedirect;
    }

    @Expose
    public CommandOutput output() {
        return new CommandOutput(this);
    }

    abstract public Stream<? extends CommandComponent> allComponents();

    @Expose("add_dependency")
    public void addDependency(Dependency dependency) {
        dependencies.add(dependency);
    }

    public Stream<? extends Dependency> dependencies() {
        return dependencies.stream();
    }

    public void setStandardInput(CommandOutput standardInput) {
        this.standardInput = standardInput;
    }

    public CommandOutput getStandardInput() {
        return standardInput;
    }

    abstract public List<AbstractCommand> reorder();

    public boolean needsProtection() {
        return false;
    }

    /**
     * Return a streams of all command
     * @return A stream of all the command contained in this command (this command included)
     */
    abstract public Stream<AbstractCommand> commands();

    /**
     * Simplify the command
     * @return A simplified command
     */
    public AbstractCommand simplify() {
        return this;
    }

    /**
     * Copy our settings to a new command
     * @param command The command
     */
    protected void copyToCommand(AbstractCommand command) {
        dependencies.forEach(command::addDependency);
        if (command.getStandardInput() == null) {
            command.setStandardInput(getStandardInput());
        }

        if (command.outputRedirect == null) {
            command.outputRedirect = outputRedirect;
        }
        if (command.errorRedirect == null) {
            command.errorRedirect = errorRedirect;
        }
    }

    public void setErrorRedirect(Redirect errorRedirect) {
        this.errorRedirect = errorRedirect;
    }

    public void setOutputRedirect(Redirect outputRedirect) {
        this.outputRedirect = outputRedirect;
    }
}