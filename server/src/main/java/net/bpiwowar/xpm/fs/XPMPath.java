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

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterables;
import net.bpiwowar.xpm.exceptions.XPMIllegalArgumentException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.utils.Output;
import org.apache.commons.lang3.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Stack;

import static java.lang.String.format;

/**
 * An XPM path
 */
public class XPMPath implements Path {
    public static final String STEP_BACK = "..";
    /**
     * The node
     */
    private final String node;

    /**
     * The share
     */
    private final String share;

    /**
     * The parts
     */
    private final String[] parts;

    /**
     * true if absolute path
     */
    private boolean absolute;

    XPMPath(String node, String share, boolean absolute, String... parts) {
        this.node = node;
        this.share = share;
        this.absolute = absolute;
        this.parts = parts;
    }

    @Override
    public FileSystem getFileSystem() {
        return XPMFileSystem.instance;
    }

    @Override
    public boolean isAbsolute() {
        return absolute;
    }

    @Override
    public Path getRoot() {
        return new XPMPath(node, share, true);
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
        return new XPMPath(node, share, absolute, Arrays.copyOfRange(parts, 0, parts.length - 1));
    }

    @Override
    public int getNameCount() {
        return parts.length;
    }

    @Override
    public Path getName(int index) {
        throw new NotImplementedException("getName");
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        throw new NotImplementedException("subpath");
    }

    @Override
    public boolean startsWith(Path other) {
        throw new NotImplementedException("startsWith");
    }

    @Override
    public boolean startsWith(String other) {
        throw new NotImplementedException("startsWith");
    }

    @Override
    public boolean endsWith(Path other) {
        throw new NotImplementedException("endsWith");
    }

    @Override
    public boolean endsWith(String other) {
        throw new NotImplementedException("endsWith");
    }

    @Override
    public Path normalize() {
        Stack<String> normalizedParts = new Stack<>();
        for (String part : parts) {
            switch (part) {
                case ".":
                    break;
                case STEP_BACK:
                    normalizedParts.pop();
                    break;
                default:
                    normalizedParts.add(part);
            }
        }
        return new XPMPath(node, share, absolute, normalizedParts.toArray(new String[normalizedParts.size()]));
    }

    @Override
    public Path resolve(Path other) {
        if (other.isAbsolute()) {
            if (other instanceof XPMPath) {
                return other;
            } else {
                return new XPMPath(node, share, false, other.toString().split(XPMFileSystem.PATH_SEPARATOR + "+"));
            }
        } else if (other.getNameCount() == 0) {
            return this;
        }

        int count = other.getNameCount();
        String[] combined = new String[parts.length + count];
        System.arraycopy(parts, 0, combined, 0, parts.length);
        int index = parts.length;
        for (Path otherPart : other) {
            combined[index++] = otherPart.toString();
        }
        return new XPMPath(node, share, absolute, combined);
    }

    @Override
    public Path resolve(String other) {
        if (other.isEmpty()) return this;

        final String[] names = other.split("/");

        // Absolute path
        if (names[0].isEmpty()) {
            new XPMPath(node, share, true, names);
        }

        // Relative path
        String[] otherParts = new String[this.parts.length + names.length];
        System.arraycopy(this.parts, 0, otherParts, 0, this.parts.length);
        System.arraycopy(names, 0, otherParts, this.parts.length, names.length);

        return new XPMPath(node, share, absolute, otherParts);
    }

    @Override
    public Path resolveSibling(Path other) {
        throw new NotImplementedException("resolveSibling");
    }

    @Override
    public Path resolveSibling(String other) {
        throw new NotImplementedException("resolveSibling");
    }

    @Override
    public Path relativize(Path other) {
        if (other == null) {
            throw new NullPointerException();
        }
        if (!(other instanceof XPMPath)) {
            throw new ProviderMismatchException();
        }

        XPMPath unixOther = (XPMPath) other;

        checkCompatible(unixOther);

        if (isAbsolute() && !unixOther.isAbsolute()) {
            throw new IllegalArgumentException("this and other must have same isAbsolute");
        }


        if (getNameCount() == 0) {
            return unixOther;
        }

        // Compute common prefix length
        int cpl = 0;
        final int l1 = parts.length;
        final int l2 = unixOther.parts.length;
        while (cpl < l1 && cpl < l2 && parts[cpl].equals(unixOther.parts[cpl])) {
            ++cpl;
        }

        // We have to take l1 - cpl step back and then l2 - cpl
        int size = l1 + l2 - 2 * cpl;
        String[] _parts = new String[size];

        if (size == l1 && size == l2) {
            // same paths: return any
            return unixOther;
        }

        // copy common part from this
        int offset = 0;
        for (int i = 0; i < l1 - cpl; ++i) {
            _parts[offset++] = STEP_BACK;
        }
        for (int i = cpl; i < l2; ++i) {
            _parts[offset++] = unixOther.parts[i];
        }

        return new XPMPath(node, share, false, _parts);
    }

    private void checkCompatible(XPMPath unixOther) {
        if (!unixOther.node.equals(node)) {
            throw new XPMIllegalArgumentException("The two paths are not on the same node [%s and %s]", unixOther.node, node);
        }

        if (!unixOther.share.equals(share)) {
            throw new XPMIllegalArgumentException("The two paths are not on the same share [%s and %s]", unixOther.share, share);
        }
    }

    @Override
    public URI toUri() {
        try {
            String ssp = node + ":" + share + ":";
            if (isAbsolute()) ssp = ssp + "/";
            ssp = ssp + Output.toString(XPMFileSystem.PATH_SEPARATOR, parts);
            return new URI(XPMFileSystemProvider.SCHEME, ssp, null);
        } catch (URISyntaxException e) {
            throw new XPMRuntimeException(e, "Could not build an URI");
        }
    }

    @Override
    public Path toAbsolutePath() {
        if (isAbsolute()) {
            return this;
        } else {
            return getRoot().resolve(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XPMPath xpmPath = (XPMPath) o;

        if (!node.equals(xpmPath.node) || !share.equals(xpmPath.share)) {
            return false;
        }

        // If one path is absolute, convert it to absolute before comparing
        if (xpmPath.isAbsolute()) {
            if (!isAbsolute()) {
                return toAbsolutePath().equals(o);
            }
        } else {
            if (isAbsolute()) {
                return xpmPath.toAbsolutePath().equals(this);
            }
        }
        return Arrays.equals(parts, xpmPath.parts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, share, parts, absolute);
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        return XPMFileSystemProvider.instance.resolvePath(this);
    }

    @Override
    public File toFile() {
        throw new NotImplementedException("toFile");
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        return XPMFileSystemProvider.instance.resolvePath(this).register(watcher, events, modifiers);
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        return XPMFileSystemProvider.instance.resolvePath(this).register(watcher, events);
    }

    @Override
    public Iterator<Path> iterator() {
        return new AbstractIterator<Path>() {
            int i = 0;

            @Override
            protected Path computeNext() {
                if (i == parts.length) {
                    return endOfData();
                }
                return new XPMPath(node, share, false, parts[i++]);
            }
        };
    }

    @Override
    public int compareTo(Path other) {
        throw new NotImplementedException("compareTo");
    }

    public String getHostName() {
        return node;
    }

    public String getShareName() {
        return share;
    }

    @Override
    public String toString() {
        if (absolute) {
            return format("shares:%s:%s:%s%s", node, share, XPMFileSystem.PATH_SEPARATOR, Output.toString(XPMFileSystem.PATH_SEPARATOR, parts));
        }
        return format("%s", Output.toString(XPMFileSystem.PATH_SEPARATOR, parts));
    }

    public String getLocalPath() {
        return Output.toString(XPMFileSystem.PATH_SEPARATOR, parts);
    }

    /**
     * Returns the contextualized path
     *
     * @param path The normalized base path
     * @return The full path
     */
    public String getLocalStringPath(String path) {
        assert path.charAt(0) == '/';
        String[] parts = path.split(XPMFileSystem.PATH_SEPARATOR + "+", 0);

        return XPMFileSystem.PATH_SEPARATOR + Output.toString(XPMFileSystem.PATH_SEPARATOR,
                Iterables.concat(Arrays.asList(parts), Arrays.asList(this.parts)));
    }
}
