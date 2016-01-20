package net.bpiwowar.xpm.utils.gson;

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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.bpiwowar.xpm.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A JSON adapter
 */
public class JsonPathAdapter extends TypeAdapter<Path> {
    // Base path for relative paths
    Path basepath;

    public JsonPathAdapter(Path basepath) {
        this.basepath = basepath;
    }

    public JsonPathAdapter() {
    }

    @Override
    public void write(JsonWriter out, Path value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            String string = PathUtils.normalizedString(value);
            out.value(string);
        }
    }

    @Override
    public Path read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) return null;
        final String str = in.nextString();
        return PathUtils.toPath(str, basepath);
    }

}
