/*
 * This file is part of experimaestro.
 * Copyright (c) 2013 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.manager.json;

import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.scheduler.Scheduler;

import java.util.HashMap;
import java.util.Map;

/**
 * A JSON object (associates a key to a json value)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 1/4/13
 */
public class JsonObject extends HashMap<String, Json> implements Json {
    public JsonObject(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public JsonObject(int initialCapacity) {
        super(initialCapacity);
    }

    public JsonObject() {
    }

    public JsonObject(Map<? extends String, ? extends Json> m) {
        super(m);
    }

    @Override
    public Json clone() {
        return new JsonObject(this);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public Object get() {
        QName parsedType = type();

        Json value = this.get(Manager.XP_VALUE.toString());
        if (value == null)
            throw new IllegalArgumentException("No value in the Json object");

        switch (parsedType.getNamespaceURI()) {

            case Manager.EXPERIMAESTRO_NS:
                switch (parsedType.getLocalPart()) {
                    case "string":
                        if (!(value instanceof JsonString))
                            throw new AssertionError("json value is not a string but" + value.getClass());
                        return value.get();

                    case "real":
                        if (!(value instanceof JsonReal))
                            throw new AssertionError("json value is not a real number but" + value.getClass());
                        return value.get();

                    case "integer":
                        if (!(value instanceof JsonInteger))
                            throw new AssertionError("json value is not an integer but " + value.getClass());
                        return value.get();

                    case "boolean":
                        if (!(value instanceof JsonBoolean))
                            throw new AssertionError("json value is not a boolean but" + value.getClass());
                        return value.get();

                    // TODO: do those checks
                    case "directory":
                    case "file":
                        try {
                            return Scheduler.getVFSManager().resolveFile(value.get().toString());
                        } catch (FileSystemException e) {
                            throw new ExperimaestroRuntimeException(e);
                        }
                    default:
                        throw new ExperimaestroRuntimeException("Un-handled type [%s]", parsedType);
                }

            default:
                throw new ExperimaestroRuntimeException("Un-handled type [%s]", parsedType);
        }
    }

    public void put(String key, String string) {
        put(key, new JsonString(string));
    }

    @Override
    public QName type() {
        Json type = get(Manager.XP_TYPE.toString());
        if (type == null)
            return Manager.XP_OBJECT;

        if (!(type instanceof JsonString))
            throw new IllegalArgumentException("No type in the Json object");

        return QName.parse(type.toString());
    }

}
