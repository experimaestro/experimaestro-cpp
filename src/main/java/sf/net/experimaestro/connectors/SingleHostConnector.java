package sf.net.experimaestro.connectors;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.UnlockableException;

/**
 * A connector that corresponds to a single host.
 *
 * Descendant of this class of connector provide access to a file system and to a process launcher.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
abstract public class SingleHostConnector extends Connector {
    /**
     * Underlying filesystem
     */
    transient private FileSystem filesystem;

    public SingleHostConnector(String id) {
        super(id);
    }

    protected SingleHostConnector() {
    }


    @Override
    public SingleHostConnector getConnector(ComputationalRequirements requirements) {
        // By default, returns ourselves - TODO: check the requirements
        return this;
    }

    @Override
    public SingleHostConnector getMainConnector() {
        // By default, the main connector is ourselves
        return this;
    }

    /**
     * Returns a new process builder
     * @return A process builder
     */
    protected abstract XPMProcessBuilder processBuilder();


    /**
     * Get the underlying filesystem
     */
    protected abstract FileSystem doGetFileSystem() throws FileSystemException;

    /**
     * Get the underlying file system
     *
     * @return
     * @throws FileSystemException
     */
    public FileSystem getFileSystem() throws FileSystemException {
        if (filesystem == null)
            return filesystem = doGetFileSystem();
        return filesystem;
    }


    /**
     * Resolve the file name
     *
     * @param path The path to the file
     * @return A file object
     * @throws FileSystemException
     */
    public FileObject resolveFile(String path) throws FileSystemException {
        return getFileSystem().resolveFile(path);
    }

    public abstract Lock createLockFile(String path) throws UnlockableException;
}
