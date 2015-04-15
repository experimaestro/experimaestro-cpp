package sf.net.experimaestro.manager.json;

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
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.ValueType;
import sf.net.experimaestro.scheduler.Resource;

import java.io.IOException;
import java.io.Writer;

import static java.lang.String.format;

/**
 * Json wrapper over resources
 */
public class JsonResource implements Json {
    Resource resource;

    public JsonResource(Resource resource) {
        this.resource = resource;
    }

    @Override
    public JsonResource clone() {
        return new JsonResource(resource);
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public Resource get() {
        return resource;
    }

    @Override
    public QName type() {
        return ValueType.XP_RESOURCE;
    }

    @Override
    public boolean canIgnore(JsonWriterOptions options) {
        return options.ignore.contains(ValueType.XP_RESOURCE);
    }

    @Override
    public void writeDescriptorString(Writer writer, JsonWriterOptions options) throws IOException {
        if (options.ignore.contains(ValueType.XP_RESOURCE)) {
            writer.write("null");
        } else {
            write(writer);
        }
    }

    @Override
    public void writeDescriptorString(Writer writer) throws IOException {

    }

    @Override
    public void write(Writer out) throws IOException {
        out.write(format("{ \"id\": \"%s\", \"$type\": \"%s\" }",
                resource.getPath().toString(),
                ValueType.XP_RESOURCE.toString()
        ));
    }

    @Override
    public void write(JsonWriter out) throws IOException {
        out.beginObject();

        out.name(Manager.XP_VALUE.toString());
        out.value(resource.getPath().toString());

        out.name(Manager.XP_TYPE.toString());
        out.value(Manager.XP_RESOURCE.toString());

        out.endObject();
    }

}
