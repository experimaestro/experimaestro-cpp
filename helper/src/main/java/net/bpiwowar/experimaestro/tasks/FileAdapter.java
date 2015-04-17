package net.bpiwowar.experimaestro.tasks;

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

import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.IOException;

/**
 * An adapter for files
 */
public class FileAdapter extends ReaderTypeAdapter<File> {

    public static final String FILE_PROTOCOL = "file://";

    @Override
    public File read(JsonReader in) throws IOException {
        final String s = in.nextString();

        if (s.startsWith(FILE_PROTOCOL)) {
            return new File(s.substring(FILE_PROTOCOL.length()));
        }

        return new File(s);
    }
}
