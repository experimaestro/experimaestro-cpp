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
import com.google.common.collect.Iterators;
import org.apache.commons.lang.NotImplementedException;
import sf.net.experimaestro.utils.Output;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Iterator;

import static java.lang.String.format;

/**
 * An XPM path
 */
public class XPMPath implements Path {
    private final String host;
    private final String share;
    private final String[] parts;

    public XPMPath(XPMFileSystem fileSystem, String host, String path) {
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
        throw new NotImplementedException();
    }

    @Override
    public Path getParent() {
        throw new NotImplementedException();
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
        throw new NotImplementedException();
    }

    @Override
    public Path resolve(Path other) {
        throw new NotImplementedException();
    }

    @Override
    public Path resolve(String other) {
        throw new NotImplementedException();
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
        throw new NotImplementedException();
    }

    @Override
    public Path toAbsolutePath() {
        throw new NotImplementedException();
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
        return format("shares://%s/%s", host, share, Output.toString(XPMFileSystem.PATH_SEPARATOR, parts));
    }

    public String getLocalPath() {
        return Output.toString(XPMFileSystem.PATH_SEPARATOR, parts);
    }

    /**
     * Returns the contextualized path
     * @param path The base path
     * @return The full path
     */
    public String getLocalStringPath(String path) {
        String[] parts = path.split(XPMFileSystem.PATH_SEPARATOR + "+", 0);

        return XPMFileSystem.PATH_SEPARATOR + Output.toString(XPMFileSystem.PATH_SEPARATOR,
                Iterables.concat(Arrays.asList(parts), Arrays.asList(this.parts)));
    }
}
