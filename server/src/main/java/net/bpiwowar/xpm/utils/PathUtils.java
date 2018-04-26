/*
 *
 *  * This file is part of experimaestro.
 *  * Copyright (c) 2016 B. Piwowarski <benjamin@bpiwowar.net>
 *  *
 *  * experimaestro is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * experimaestro is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package net.bpiwowar.xpm.utils;

import net.bpiwowar.xpm.exceptions.XPMRuntimeException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;

/**
 * java.nio related utils
 */
public class PathUtils {
    public static final String QUOTED_SPECIAL = "\"$";
    public static final String SHELL_SPECIAL = " \\;\"'<>\n$()";

    /**
     * Returns a string that uniquely represents a given path
     * @param value The path to transform
     * @return A string representing the path
     */
    public static String normalizedString(Path value) {
        String string = value.toAbsolutePath().toUri().toString();
        if (string.endsWith("/")) {
            string = string.substring(0, string.length() - 1);
        }
        return string;
    }

    static public String protect(String string, String special) {
        if (string.equals(""))
            return "\"\"";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            final char c = string.charAt(i);
            if (special.indexOf(c) != -1)
                sb.append("\\");
            sb.append(c);
        }
        return sb.toString();
    }

    public static Path toPath(String path) throws IOException {
        return toPath(path, null);
    }

    /**
     * Converts a sring to a path
     * @param path A path that can be either relative or an URI
     * @param basepath The basepath (in case of relative values)
     * @return A path
     * @throws IOException If the URI is wrong
     */
    public static Path toPath(String path, Path basepath) throws IOException {
        // Case of an absolute local path
        if (path.startsWith("/")) {
            return Paths.get(path);
        }

        final URI uri;
        try {
            uri = new URI(path);
        } catch (URISyntaxException e) {
            throw new IOException("Could not decode " + path + " as URI", e);
        }

        if (uri.getScheme() == null && basepath != null) {
            return basepath.resolve(path);
        }
        try {
            return Paths.get(uri);
        } catch(RuntimeException e) {
            throw new XPMRuntimeException("Could not convert URI " + uri + " to a path: " + e.getLocalizedMessage());
        }
    }

    public static String quotedProtect(String email) {
        return protect(email, QUOTED_SPECIAL);
    }
}
