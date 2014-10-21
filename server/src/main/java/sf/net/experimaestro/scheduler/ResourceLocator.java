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

package sf.net.experimaestro.scheduler;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.KeyField;
import com.sleepycat.persist.model.Persistent;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.utils.FileNameTransformer;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Identifies a resource on the network.
 * <p/>
 * In practice, it is an URI
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/6/12
 */
@Persistent
public class ResourceLocator implements Comparable<ResourceLocator> {
    @KeyField(value = 1)
    String connectorId;

    @KeyField(value = 2)
    String path;

    /**
     * The underlying connector, initialized with {@linkplain #init(Scheduler)}
     */
    transient Connector connector;


    protected ResourceLocator() {
        this.connectorId = "";
        this.path = "";
    }

    /**
     * Initialization with a connector id. The object needs to be initialized
     * with {@linkplain #init(Scheduler)}
     *
     * @param connectorId The path to the resource on the main connector
     */
    public ResourceLocator(String connectorId) {
        this.connectorId = connectorId;
        this.path = "/";
    }


    public ResourceLocator(ResourceLocator other) {
        this.connectorId = other.connectorId;
        this.connector = other.connector;
        this.path = other.path;
    }

    public ResourceLocator(Connector connector, String path) {
        this.connectorId = connector.getIdentifier();
        this.path = path;
        this.connector = connector;
    }

    /**
     * Init from a pair of strings
     * <p/>
     * <b>Note</b> that the object needs to be initialized if used for something else
     * than retrieving a record
     *
     * @param connectorId
     * @param path
     */
    public ResourceLocator(String connectorId, String path) {
        this.connectorId = connectorId;
        this.path = path;
    }

    public static ResourceLocator parse(String idString) {
        try {
            final URI uri = new URI(idString);
            StringBuilder connectorId = new StringBuilder();
            StringBuilder path = new StringBuilder();

            connectorId.append(uri.getScheme());
            connectorId.append("://");
            if (uri.getRawAuthority() != null)
                connectorId.append(uri.getRawAuthority());

            path.append(uri.getRawPath());

            return new ResourceLocator(connectorId.toString(), path.toString());
        } catch (URISyntaxException e) {
            throw new XPMRuntimeException("Could not parse locator URI [%s]", idString);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceLocator that = (ResourceLocator) o;

        if (!connectorId.equals(that.connectorId)) return false;
        if (!path.equals(that.path)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = connectorId.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }

    public void init(Scheduler scheduler) throws DatabaseException {
        connector = scheduler.getConnector(connectorId);
    }

    public String getPath() {
        return path;
    }

    @Override
    public int compareTo(ResourceLocator other) {
        if (other == null)
            return 1;

        int z = connectorId.compareTo(other.connectorId);

        if (z != 0)
            return z;
        return path.compareTo(other.path);
    }

    @Override
    public String toString() {
        return String.format("%s%s", connectorId, path);
    }

    public String getConnectorId() {
        return connectorId;
    }

    public Connector getConnector() {
        if (connector == null)
            throw new XPMRuntimeException("The locator %s has not been properly initialized", connectorId);

        return connector;
    }

    /**
     * Resolve a path relative to this resource
     *
     * @param path   The path
     * @param parent Is the path relative to the parent?
     * @return A new resource from object
     * @throws FileSystemException If something goes wrong while accessing the file system
     */
    public ResourceLocator resolvePath(String path, boolean parent) throws FileSystemException {
        // Get the current file
        FileObject file = getFile();

        // Get the target (revert to file if null)
        FileObject target = (parent ? file.getParent() : file);
        if (target == null)
            target = file;

        target = target.resolveFile(path);
        return new ResourceLocator(connector, target.getName().getPath());
    }

    public ResourceLocator resolvePath(String path) throws FileSystemException {
        return resolvePath(path, false);
    }

    public FileObject getFile() throws FileSystemException {
        return connector.getMainConnector().resolveFile(path);
    }


    /**
     * Returns the path of the file relative to the a given's host filesystem, adding an extension
     *
     * @param connector A single host connector
     * @param extension The extension
     * @return A file object
     * @throws FileSystemException
     */
    public FileObject resolve(SingleHostConnector connector, String extension) throws FileSystemException {
        return resolvePath(connector, this.path + extension);
    }

    /**
     * Resolve a path relative to a given File Object
     *
     * @param connector
     * @param path      The path to the resource
     * @return
     */
    private FileObject resolvePath(SingleHostConnector connector, String path) throws FileSystemException {
        if (this.connector == connector)
            return connector.resolveFile(path);
        throw new NotImplementedException();
    }

    public FileObject resolve(SingleHostConnector connector, FileNameTransformer transformer) throws FileSystemException {
        return transformer.transform(resolvePath(connector, this.path));
    }
}
