package net.bpiwowar.xpm.connectors;

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

import net.bpiwowar.xpm.commands.Commands;

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
     * The script file
     */
    protected Path scriptFile;
    /**
     * Local path to the script file
     */
    protected String path;
    /**
     * The environment
     */
    private Map<String, String> environment;
    /**
     * Commands
     */
    private Commands commands;

    /**
     * The notification URL if any
     */
    protected URL notificationURL;

    public XPMScriptProcessBuilder(Launcher launcher, Path scriptFile, AbstractProcessBuilder processBuilder) throws IOException {
        this.launcher = launcher;
        this.scriptFile = scriptFile;
        this.path = launcher.getConnector().resolve(scriptFile);
        this.processBuilder = processBuilder == null ? launcher.processBuilder() : processBuilder;
    }

    /**
     * Sets the notification URL
     */
    public void notificationURL(URL url) {
        this.notificationURL = url;
    }

    /**
     * Sets the commands
     */
    public void commands(Commands commands) {
        this.commands = commands;
    }

    public Commands commands() {
        return commands;
    }

    public abstract void removeLock(Path lockFile) throws IOException;

    public abstract void exitCodeFile(Path exitCodeFile) throws IOException;

    public abstract void doneFile(Path doneFile) throws IOException;

    public URL getNotificationURL() {
        return notificationURL;
    }

    public void setNotificationURL(URL notificationURL) {
        this.notificationURL = notificationURL;
    }


}
