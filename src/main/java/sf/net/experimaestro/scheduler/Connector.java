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

package sf.net.experimaestro.scheduler;

import com.jcraft.jsch.JSchException;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.UnlockableException;

import java.io.*;
import java.util.ArrayList;

/**
 * This class represents any layer that can get between a host where a command is executed
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/6/12
 */
@Entity
public abstract class Connector implements Comparable<Connector> {
    @PrimaryKey
    String identifier;

    protected Connector() {
    }

    protected Connector(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Creates a writer stream for a given identifier
     *
     *
     * @param path
     * @return A valid object
     */
    abstract PrintWriter printWriter(String path) throws Exception;

    /** Execute a command */
    abstract JobMonitor exec(Job job, String command, ArrayList<Lock> locks) throws Exception;

    abstract Lock createLockFile(String path) throws UnlockableException;

    abstract void touchFile(String path) throws Exception;

    abstract boolean fileExists(String path) throws Exception;

    abstract long getLastModifiedTime(String path) throws Exception;

    abstract InputStream getInputStream(String path) throws Exception;

    abstract void renameFile(String from, String to) throws Exception;

    abstract void setExecutable(String path, boolean flag) throws Exception;

    /** Returns the connectorId identifier */
    final String getIdentifier() {
        return identifier;
    }

}
