package sf.net.experimaestro.scheduler;

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

import sf.net.experimaestro.annotations.Expose;
import sf.net.experimaestro.annotations.Exposed;
import sf.net.experimaestro.connectors.AbstractCommandBuilder;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Created by bpiwowar on 11/02/15.
 */
@Exposed
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
     * Process each dependency contained in a command or subcommand
     * @param consumer The consumer to be fed
     */
    abstract public void forEachDependency(Consumer<Dependency> consumer);

    /**
     * Process each command
     * @param consumer The consumer to be fed
     */
    public abstract void forEachCommand(Consumer<? super AbstractCommand> consumer);

    public abstract void prepare(CommandContext env);

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
}
