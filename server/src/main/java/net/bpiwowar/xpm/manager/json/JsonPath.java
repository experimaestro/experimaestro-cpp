package net.bpiwowar.xpm.manager.json;

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

import com.google.gson.stream.JsonWriter;
import org.json.simple.JSONValue;
import net.bpiwowar.xpm.manager.Constants;
import net.bpiwowar.xpm.manager.TypeName;
import net.bpiwowar.xpm.manager.scripting.Exposed;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;

import static java.lang.String.format;

/**
 * The default interface for JsonPath
 */
@Exposed
public class JsonPath extends Json {
    private Path path;

    private JsonPath() {
    }

    public JsonPath(Path path) {
        this.path = path;
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public Path get() {
        return path;
    }

    @Override
    public TypeName type() {
        return Constants.XP_PATH;
    }

    @Override
    public boolean canIgnore(JsonWriterOptions options) {
        return options.ignore.contains(Constants.XP_PATH);
    }

    @Override
    public String toString() {
        return format("Path(%s)", path.toString());
    }

    @Override
    public void writeDescriptorString(Writer writer, JsonWriterOptions options) throws IOException {
        if (options.ignore.contains(Constants.XP_PATH) || path == null) {
            writer.write("null");
        } else {
            writer.write('"');
            writer.write(JSONValue.escape(options.resolver.apply(path)));
            writer.write('"');
        }
    }

    @Override
    public void write(Writer out) throws IOException {
        out.write('"');
        out.write(JSONValue.escape(get().toString()));
        out.write('"');
    }

    @Override
    public void write(JsonWriter out) throws IOException {
        out.beginObject();

        out.name(Constants.XP_VALUE.toString());
        out.value(get().toUri().toString());

        out.name(Constants.XP_TYPE.toString());
        out.value(Constants.XP_PATH.toString());

        out.endObject();
    }
}
