package sf.net.experimaestro.connectors;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.UnlockableException;

/**
 * A connector that corresponds to a single host.
 *
 * Descendant of this class of connector provide access to a file system and to a process launcher.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
abstract public class SingleHostConnector extends Connector implements Launcher {
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

    /**
     * Resolve a FileObject to a local path
     */
    public String resolve(FileObject file) {
        try {
            return file.getURL().getPath();
        } catch (FileSystemException e) {
            throw new ExperimaestroRuntimeException("Could not convert the URL [%s] to a local path for %s", file.toString(), toString());
        }
    }


    /** Returns a process builder */
    public abstract XPMProcessBuilder processBuilder();

    /** Lock a file */
    public abstract Lock createLockFile(String path) throws UnlockableException;

    /** Returns the hostname */
    public abstract String getHostName();

    @Override
    public XPMProcessBuilder processBuilder(SingleHostConnector connector) {
        if (connector != this)
            throw new ExperimaestroRuntimeException("");

        return this.processBuilder();
    }
}
