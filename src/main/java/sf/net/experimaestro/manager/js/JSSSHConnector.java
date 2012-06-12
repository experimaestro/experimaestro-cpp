/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package sf.net.experimaestro.manager.js;

import com.jcraft.jsch.JSchException;
import com.sleepycat.je.DatabaseException;
import org.mozilla.javascript.*;
import sf.net.experimaestro.locks.LockType;
import sf.net.experimaestro.scheduler.*;
import sf.net.experimaestro.utils.log.Logger;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * Scheduler as seen by JavaScript
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSSSHConnector extends ScriptableObject implements JSConnector {
    final static private Logger LOGGER = Logger.getLogger();

    private static final long serialVersionUID = 1L;

    public static final String CLASSNAME = "SSHConnector";

    Connector connector;

    public JSSSHConnector() {
    }

    public void jsConstructor(String username, String hostname) throws JSchException {
        this.connector = new SSHConnector(username, hostname);
    }

    @Override
    public String getClassName() {
        return CLASSNAME;
    }

    @Override
    public Connector getConnector() {
        return connector;
    }

    // ---- JavaScript functions ----



}