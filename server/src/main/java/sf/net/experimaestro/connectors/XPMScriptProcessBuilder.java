/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.connectors;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.scheduler.Commands;

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
    protected SingleHostConnector connector;
    /**
     * The script file
     */
    protected FileObject scriptFile;
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

    public XPMScriptProcessBuilder(SingleHostConnector connector, FileObject scriptFile, AbstractProcessBuilder processBuilder) throws FileSystemException {
        this.connector = connector;
        this.scriptFile = scriptFile;
        this.path = connector.resolve(scriptFile);
        this.processBuilder = processBuilder == null ? connector.processBuilder() : processBuilder;
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

    public abstract void removeLock(FileObject lockFile) throws FileSystemException;

    public abstract void exitCodeFile(FileObject exitCodeFile) throws FileSystemException;

    public abstract void doneFile(FileObject doneFile) throws FileSystemException;
}
