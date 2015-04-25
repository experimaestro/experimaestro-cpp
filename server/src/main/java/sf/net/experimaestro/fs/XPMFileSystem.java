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

import com.pastdev.jsch.file.UnixSshPath;
import org.apache.commons.lang.NotImplementedException;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;

/**
 * The experimaestro file system
 */
public class XPMFileSystem extends FileSystem {
    public static final String PATH_SEPARATOR = "/";
    public static XPMFileSystem instance = new XPMFileSystem();


    @Override
    public FileSystemProvider provider() {
        return XPMFileSystemProvider.instance;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getSeparator() {
        return PATH_SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        throw new NotImplementedException();
    }

    /**
     * Each store corresponds to one network share
     * @return
     */
    @Override
    public Iterable<FileStore> getFileStores() {
        // Should return the list of network shares
        throw new NotImplementedException();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        throw new NotImplementedException();
    }

    @Override
    public Path getPath(String first, String... more) {
        if ( more == null || more.length == 0 ) {
            return new XPMPath(this, null, first );
        }

        StringBuilder builder = new StringBuilder( first );
        for ( String part : more ) {
            builder.append( PATH_SEPARATOR )
                    .append( part );
        }
        return new XPMPath(this, null, builder.toString());
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new NotImplementedException();
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new NotImplementedException();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new NotImplementedException();
    }

    public Path getPath(URI uri) {
        return new XPMPath(this, uri.getHost(), uri.getPath());
    }
}
