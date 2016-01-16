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

import java.nio.file.FileSystemException;
import java.nio.file.Path;

/**
 * Transforms the filename
 */
final public class FileNameTransformer {
    final String prefix;
    final String suffix;

    public FileNameTransformer(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public Path transform(Path path)  {
        final String baseName = prefix + path.getFileName().toString() + suffix;
        return path.getParent().resolve(baseName);
    }
}
