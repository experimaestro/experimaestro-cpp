package net.bpiwowar.xpm.fs;

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

import org.apache.commons.lang.NotImplementedException;
import net.bpiwowar.xpm.connectors.NetworkShare;
import net.bpiwowar.xpm.connectors.NetworkShareAccess;
import net.bpiwowar.xpm.connectors.SingleHostConnector;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

/**
 * The XPM file system provider
 */
public class XPMFileSystemProvider extends FileSystemProvider {

    public static final String SCHEME = "shares";
    final static private Logger LOGGER = Logger.getLogger();
    public static XPMFileSystemProvider instance = new XPMFileSystemProvider();

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        return null;
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        return XPMFileSystem.instance;
    }

    @Override
    public Path getPath(URI uri) {
        return XPMFileSystem.instance.getPath(uri);
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        final Path hostPathObject = resolvePath(path);


        SeekableByteChannel channel = Files.newByteChannel(hostPathObject, options, attrs);
        if (channel == null) {
            throw new IOException(format("Could not find a valid mount point for %s", path));
        }

        return channel;
    }

    Path resolvePath(Path path) {
        XPMPath _path = (XPMPath) path;

        NetworkShareAccess[] accesses = getNetworkShareAccesses(_path);
        for (NetworkShareAccess access : accesses) {
            final SingleHostConnector connector = access.getConnector();
            final String hostPath = access.getPath();
            try {
                return connector
                        .resolveFile(hostPath)
                        .resolve(_path.getLocalPath())
                        .normalize();

            } catch (IOException e) {
                LOGGER.error(e, "Error trying to access %s from %s", hostPath, connector);
            }
        }
        return null;
    }

    static public NetworkShareAccess[] getNetworkShareAccesses(XPMPath _path) {
        NetworkShare share = null;
        try {
            share = NetworkShare.find(_path.getHostName(), _path.getShareName());
        } catch (SQLException e) {
            throw new XPMRuntimeException("Could not find shares://%s/%s", _path.getHostName(), _path.getShareName());
        }
        NetworkShareAccess accesses[] = share.getAccess().toArray(new NetworkShareAccess[0]);
        Arrays.sort(accesses, (o1, o2) -> Long.compare(o2.getPriority(), o1.getPriority()));
        return accesses;
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        return Files.newDirectoryStream(resolvePath(dir), filter);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        Path path = resolvePath(dir);
        Files.createDirectory(path);
    }

    @Override
    public void delete(Path path) throws IOException {
        Files.delete(resolvePath(path));
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        Files.copy(resolvePath(source), resolvePath(target), options);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        Files.move(resolvePath(source), resolvePath(target), options);
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return Files.isSameFile(resolvePath(path), resolvePath(path2));
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return Files.isHidden(resolvePath(path));
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        final Path _path = resolvePath(path);
        _path.getFileSystem().provider().checkAccess(_path, modes);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return Files.getFileAttributeView(resolvePath(path), type, options);
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        return Files.readAttributes(resolvePath(path), type, options);
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return Files.readAttributes(resolvePath(path), attributes, options);
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        Files.setAttribute(resolvePath(path), attribute, value, options);
    }
}
