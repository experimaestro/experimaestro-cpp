package sf.net.experimaestro.connectors;

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

import sf.net.experimaestro.fs.XPMPath;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;

import static java.lang.String.format;

/**
 * An access to a network share
 */
public class NetworkShareAccess implements Serializable {
    /**
     * The host that allows us to access the data
     */
    SingleHostConnector connector;

    NetworkShare share;

    /**
     * Path on the single host connector
     */
    String path;


    /**
     * The priority of this access
     */
    int priority;

    public NetworkShareAccess(NetworkShare networkShare, SingleHostConnector connector, String path, int priority) {
        share = networkShare;
        this.connector = connector;
        this.path = path;
        this.priority = priority;
    }

    public NetworkShareAccess() {
    }

    public boolean is(SingleHostConnector connector) {
        return connector.equals(this.connector);
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetworkShareAccess that = (NetworkShareAccess) o;

        if (!connector.equals(that.connector)) return false;
        return share.equals(that.share);

    }

    @Override
    public int hashCode() {
        int result = connector.hashCode();
        result = 31 * result + share.hashCode();
        return result;
    }

    public int getPriority() {
        return priority;
    }

    public SingleHostConnector getConnector() {
        return connector;
    }

    public String getPath() {
        return path;
    }

    public String resolve(XPMPath path) throws IOException {
        if (!path.getHostName().equals(share.host) || !path.getShareName().equals(share.name)) {
            throw new IllegalArgumentException(format("Cannot resolve %s for share %s", path, share));
        }
        return path.getLocalStringPath(this.path);
    }
}
