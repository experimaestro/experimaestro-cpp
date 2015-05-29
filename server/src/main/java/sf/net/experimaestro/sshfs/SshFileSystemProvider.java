package sf.net.experimaestro.sshfs;

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
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

/**
 * The SSH file system provider
 */
public class SshFileSystemProvider extends FileSystemProvider {
    final static private Logger LOGGER = Logger.getLogger();

    public static FileSystemProvider instance = new SshFileSystemProvider();

    @Override
    public String getScheme() {
        return "ssh";
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        return null;
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        return SshFileSystem.instance;
    }

    @Override
    public Path getPath(URI uri) {
        return SshFileSystem.instance.getPath(uri);
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void delete(Path path) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return false;
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        throw new NotImplementedException();
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        throw new NotImplementedException();
    }
}
