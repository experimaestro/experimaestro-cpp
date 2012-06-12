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
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.UnlockableException;

import java.io.*;
import java.util.ArrayList;

/**
 * This class represents any layer that can get between a host where a command is executed
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/6/12
 */
public interface Connector {
    /**
     * Creates a writer stream for a given identifier
     *
     *
     * @param identifier
     * @return A valid object
     */
    PrintWriter printWriter(String identifier) throws Exception;

    int exec(String identifier, String command, ArrayList<Lock> locks) throws Exception;

    Lock createLockFile(String lockIdentifier) throws UnlockableException;

    void touchFile(String identifier) throws Exception;

    boolean fileExists(String identifier) throws Exception;

    long getLastModifiedTime(String identifier) throws Exception;

    InputStream getInputStream(String identifier) throws Exception;

    void renameFile(String from, String to) throws Exception;

    void setExecutable(String path, boolean flag) throws Exception;
}
