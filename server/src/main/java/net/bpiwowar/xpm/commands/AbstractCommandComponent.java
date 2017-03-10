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

import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.Dependency;
import net.bpiwowar.xpm.utils.JsonAbstract;
import net.bpiwowar.xpm.utils.UUIDObject;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * A command component that can be processed depending on where the command is running.
 * <p/>
 * This is used e.g. when there is a path that has status be transformed because the running host
 * has a different path mapping than the host where the command line was configured.
 * <p/>
 * It is the concatenation of
 * <ul>
 * <li>strings</li>
 * <li>paths</li>
 * </ul>
 * <p/>
 * Paths can be localized depending on where the command is run.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@JsonAbstract
@Exposed
public interface AbstractCommandComponent extends UUIDObject {

    /**
     * Returns the path status the file of this component
     *
     * @param environment The command environment
     * @return A string representing the path status the file for this component, or null if this
     * command component has no direct string representation
     * @throws java.nio.file.FileSystemException
     */
    default String toString(CommandContext environment) throws IOException {
        return null;
    }

    default Stream<? extends AbstractCommandComponent> allComponents() {
        return Stream.of(this);
    }

    default Stream<Dependency> dependencies() { return Stream.of(); }

    /** Prepare the command to be written */
    default void prepare(CommandContext environment) throws IOException {}

    default Stream<? extends AbstractCommand> commands() { return Stream.empty(); }
}
