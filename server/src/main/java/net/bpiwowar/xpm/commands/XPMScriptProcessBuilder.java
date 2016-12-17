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

import net.bpiwowar.xpm.connectors.AbstractProcessBuilder;
import net.bpiwowar.xpm.connectors.Launcher;
import net.bpiwowar.xpm.scheduler.LauncherParameters;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

/**
 * An abstract class that allows building scripts in different scripting languages
 * (sh, etc.)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 10/9/12
 */
public abstract class XPMScriptProcessBuilder extends AbstractCommandBuilder {
    /**
     * The process builder
     */
    protected final AbstractProcessBuilder processBuilder;

    /**
     * The launcher
     */
    protected Launcher launcher;

    /**
     * Launcher parameters
     */
    protected final LauncherParameters parameters;

    /**
     * The script file
     */
    protected Path scriptFile;
    /**
     * Local path to the script file
     */
    protected Path path;
    /**
     * Commands
     */
    private AbstractCommand command;

    /**
     * The notification URL if any
     */
    protected URL notificationURL;

    public XPMScriptProcessBuilder(Launcher launcher, LauncherParameters parameters, Path scriptFile, AbstractProcessBuilder processBuilder) throws IOException {
        this.launcher = launcher;
        this.parameters = parameters;
        this.scriptFile = scriptFile;
        this.path = scriptFile;
        this.processBuilder = processBuilder == null ? launcher.processBuilder(parameters) : processBuilder;
    }

    /**
     * Sets the command
     */
    public void command(AbstractCommand command) {
        this.command = command;
    }

    public AbstractCommand command() {
        return this.command;
    }

    public abstract void removeLock(Path lockFile) throws IOException;

    public abstract void exitCodeFile(Path exitCodeFile) throws IOException;

    public abstract void pidFile(Path transform);

    public abstract void doneFile(Path doneFile) throws IOException;

    public URL getNotificationURL() {
        return notificationURL;
    }

    public void setNotificationURL(URL notificationURL) {
        this.notificationURL = notificationURL;
    }


}
