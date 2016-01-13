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
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.scripting.Exposed;

import java.io.IOException;
import java.io.Writer;

/**
 * Created by bpiwowar on 10/9/14.
 */
@Exposed
public class JsonNull extends Json {
    private static JsonNull singleton = new JsonNull();

    private JsonNull() {
    }

    public static JsonNull getSingleton() {
        return singleton;
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public Object get() {
        return null;
    }

    @Override
    public QName type() {
        return null;
    }

    @Override
    public boolean canIgnore(JsonWriterOptions options) {
        return true;
    }

    @Override
    public void writeDescriptorString(Writer writer, JsonWriterOptions options) throws IOException {
        writer.write("null");
    }

    @Override
    public void write(Writer out) throws IOException {
        out.write("null");
    }

    @Override
    public void write(JsonWriter out) throws IOException {
        out.nullValue();
    }
}
