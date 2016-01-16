package net.bpiwowar.xpm.utils;

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

import net.bpiwowar.xpm.utils.log.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Static methods for files
 *
 * @author B. Piwowarski
 * @date 17/11/2006
 */
public class FileSystem {
    /**
     * A filter for directories
     */
    final public static FileFilter DIRECTORY_FILTER = pathname -> pathname.isDirectory();
    /**
     * A filter for files
     */
    final public static FileFilter FILE_FILTER = pathname -> pathname.isFile();
    private static final Logger logger = Logger.getLogger();

    /**
     * Get a file filter
     *
     * @param extFilter
     * @param skipRegExp
     * @return
     */
    public static FileFilter newRegexpFileFilter(final String extFilter,
                                                 final String skipRegExp) {
        return new FileFilter() {
            final Pattern pattern = skipRegExp != null ? Pattern
                    .compile(skipRegExp) : null;

            public boolean accept(File file) {
                return (file.getName().endsWith(extFilter) && (pattern == null || !pattern
                        .matcher(file.getName()).find()));
            }
        };
    }

    /**
     * Create a new file object from a list of names
     *
     * @param names A list of strings
     */
    public static File createFileFromPath(String... names) {
        return createFileFromPath(null, names);
    }

    /**
     * Creates a file from a list of strings and a base directory
     *
     * @param baseDirectory
     * @param names
     * @return
     */
    public static File createFileFromPath(File baseDirectory, String... names) {
        for (String name : names)
            if (baseDirectory == null)
                baseDirectory = new File(name);
            else
                baseDirectory = new File(baseDirectory, name);
        return baseDirectory;
    }

    /**
     * Delete everything recursively
     *
     * @param directory The directory to delete
     */
    static public void recursiveDelete(File directory) throws IOException {
        recursiveDelete(directory.toPath());
    }

    /**
     * Delete everything recursively
     *
     * @param path
     */
    static public void recursiveDelete(Path path) throws IOException {
        logger.debug("Deleting %s", path);
        final Iterator<Path> it = Files.list(path).iterator();
        while (it.hasNext()) {
            final Path entry = it.next();

            logger.debug("Considering " + entry);

            if (Files.isDirectory(entry)) {
                recursiveDelete(entry);
            } else {
                Files.delete(entry);
            }

        }

        // Deleting self
        Files.delete(path);
    }


}
