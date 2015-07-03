package sf.net.experimaestro.fs;

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

import com.google.common.collect.Iterables;
import org.apache.commons.lang.NotImplementedException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.utils.Output;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Stack;

import static java.lang.String.format;

/**
 * An XPM path
 */
public class XPMPath implements Path {
    private final String host;

    private final String share;

    private final String[] parts;

    public XPMPath(String host, String path) {
        this.host = host;

        assert !path.isEmpty();

        String[] parts = path.split(XPMFileSystem.PATH_SEPARATOR + "+", 0);
        assert parts[0].isEmpty();

        // Get share name
        if (parts.length < 1) {
            throw new IllegalArgumentException("Hostname should be specified");
        }
        this.share = parts[1];

        // Get the rest of the path
        int newLength = parts.length - 2;
        this.parts = new String[newLength];
        System.arraycopy(parts, 2, this.parts, 0, newLength);
    }

    public XPMPath(String host, String share, String[] parts) {
        this.host = host;
        this.share = share;
        this.parts = parts;
    }

    @Override
    public FileSystem getFileSystem() {
        return XPMFileSystem.instance;
    }

    @Override
    public boolean isAbsolute() {
        return true;
    }

    @Override
    public Path getRoot() {
        throw new NotImplementedException();
    }

    @Override
    public Path getFileName() {
        // Returns the last bit
        return Paths.get(parts[parts.length - 1]);
    }

    @Override
    public Path getParent() {
        if (parts.length == 0)
            return this;
        return new XPMPath(host, share, Arrays.copyOfRange(parts, 0, parts.length - 1));
    }

    @Override
    public int getNameCount() {
        throw new NotImplementedException();
    }

    @Override
    public Path getName(int index) {
        throw new NotImplementedException();
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        throw new NotImplementedException();
    }

    @Override
    public boolean startsWith(Path other) {
        throw new NotImplementedException();
    }

    @Override
    public boolean startsWith(String other) {
        throw new NotImplementedException();
    }

    @Override
    public boolean endsWith(Path other) {
        throw new NotImplementedException();
    }

    @Override
    public boolean endsWith(String other) {
        throw new NotImplementedException();
    }

    @Override
    public Path normalize() {
        Stack<String> normalizedParts = new Stack<>();
        for(String part: parts) {
            switch (part) {
                case ".":
                    break;
                case "..":
                    normalizedParts.pop();
                    break;
                default:
                    normalizedParts.add(part);
            }
        }
        return new XPMPath(host, share, normalizedParts.toArray(new String[normalizedParts.size()]));
    }

    @Override
    public Path resolve(Path other) {
        throw new NotImplementedException();
    }

    @Override
    public Path resolve(String other) {
        if (other.isEmpty()) return this;

        final String[] names = other.split("/");

        // Absolute path
        if (names[0].isEmpty()) {
            new XPMPath(host, share, names);
        }

        // Relative path
        String[] otherParts = new String[this.parts.length + names.length];
        System.arraycopy(this.parts, 0, otherParts, 0, this.parts.length);
        System.arraycopy(names, 0, otherParts, this.parts.length, names.length);

        return new XPMPath(host, share, otherParts);
    }

    @Override
    public Path resolveSibling(Path other) {
        throw new NotImplementedException();
    }

    @Override
    public Path resolveSibling(String other) {
        throw new NotImplementedException();
    }

    @Override
    public Path relativize(Path other) {
        throw new NotImplementedException();
    }

    @Override
    public URI toUri() {
        try {
            final String path = XPMFileSystem.PATH_SEPARATOR + share + XPMFileSystem.PATH_SEPARATOR + Output.toString(XPMFileSystem.PATH_SEPARATOR, parts);
            return new URI(XPMFileSystemProvider.SCHEME, host, path, "");
        } catch (URISyntaxException e) {
            throw new XPMRuntimeException(e, "Could not build an URI");
        }
    }

    @Override
    public Path toAbsolutePath() {
        return this;
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public File toFile() {
        throw new NotImplementedException();
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public Iterator<Path> iterator() {
        throw new NotImplementedException();
    }

    @Override
    public int compareTo(Path other) {
        throw new NotImplementedException();
    }

    public String getHostName() {
        return host;
    }

    public String getShareName() {
        return share;
    }

    @Override
    public String toString() {
        return format("shares://%s/%s/%s", host, share, Output.toString(XPMFileSystem.PATH_SEPARATOR, parts));
    }

    public String getLocalPath() {
        return Output.toString(XPMFileSystem.PATH_SEPARATOR, parts);
    }

    /**
     * Returns the contextualized path
     *
     * @param path The base path (has to be absolute)
     * @return The full path
     */
    public String getLocalStringPath(String path) {
        assert path.charAt(0) == '/';
        String[] parts = path.split(XPMFileSystem.PATH_SEPARATOR + "+", 0);

        return XPMFileSystem.PATH_SEPARATOR + Output.toString(XPMFileSystem.PATH_SEPARATOR,
                Iterables.concat(Arrays.asList(parts), Arrays.asList(this.parts)));
    }
}
