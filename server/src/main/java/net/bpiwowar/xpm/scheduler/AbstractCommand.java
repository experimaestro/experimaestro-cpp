package net.bpiwowar.xpm.scheduler;

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
import net.bpiwowar.xpm.connectors.AbstractCommandBuilder;
import net.bpiwowar.xpm.utils.JsonAbstract;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * An abstract command
 */
@Exposed
@JsonAbstract
public abstract class AbstractCommand {
    /**
     * The input redirect
     * <p>
     * Null indicates that the input should be the null device
     */
    AbstractCommandBuilder.Redirect inputRedirect = null;
    /**
     * The output stream redirect.
     * <p>
     * Null indicates that the output should be discarded
     */
    AbstractCommandBuilder.Redirect outputRedirect = null;
    /**
     * The error stream redirect.
     * <p>
     * Null indicates that the output should be discarded
     */
    AbstractCommandBuilder.Redirect errorRedirect = null;

    /**
     * Standard input
     */
    Command.CommandOutput standardInput;

    /**
     * Process each dependency contained in a command or subcommand
     * @param consumer The consumer to be fed
     */
    abstract public void forEachDependency(Consumer<Dependency> consumer);

    /**
     * Process each command
     * @param consumer The consumer to be fed
     */
    public abstract void forEachCommand(Consumer<? super AbstractCommand> consumer);

    void prepare(CommandContext env) {
        if (standardInput != null) {
            try {
                standardInput.prepare(env);
            } catch (IOException e) {
                throw new XPMRuntimeException(e);
            }
        }
    }

    protected AbstractCommand() {}

    public AbstractCommandBuilder.Redirect getOutputRedirect() {
        return outputRedirect;
    }

    public AbstractCommandBuilder.Redirect getErrorRedirect() {
        return errorRedirect;
    }

    @Expose
    public Command.CommandOutput output() {
        return new Command.CommandOutput(this);
    }

    abstract public Stream<? extends CommandComponent> allComponents();

    public abstract Stream<? extends Dependency> dependencies();

    public void setStandardInput(Command.CommandOutput standardInput) {
        this.standardInput = standardInput;
    }

    public Command.CommandOutput getStandardInput() {
        return standardInput;
    }

    abstract public List<AbstractCommand> reorder();
}