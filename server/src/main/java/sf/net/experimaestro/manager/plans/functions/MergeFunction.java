package sf.net.experimaestro.manager.plans.functions;

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

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonArray;
import sf.net.experimaestro.manager.json.JsonObject;

import java.util.Iterator;
import java.util.Map;

/**
 * A function that simply merge JSON objects
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class MergeFunction implements Function {
    private final QName outputType;
    private final Int2ObjectOpenHashMap<String> map;

    public MergeFunction(QName outputType, Int2ObjectOpenHashMap<String> map) {
        this.outputType = outputType;
        this.map = map;
    }

    @Override
    public Iterator<? extends Json> apply(Json[] input) {
        JsonObject returned = new JsonObject();
        for (int i = 0; i < input.length; i++) {
            Json json = input[i];
            String oKey = map.get(i);
            if (oKey != null) {
                returned.put(oKey, json);
            } else {
                if (json instanceof JsonObject) {
                    for (Map.Entry<String, Json> entry : ((JsonObject) json).entrySet()) {
                        String key = entry.getKey();
                        if (returned.containsKey(key.toString()))
                            throw new XPMRuntimeException("Conflicting id in merge: %s", key);
                        returned.put(key, entry.getValue());
                    }
                } else {
                    XPMRuntimeException e = new XPMRuntimeException("Can only merge JSON objects, but not %s", json.getClass());
                    if (outputType != null)
                        e.addContext("while merging into JSON object of type %s", outputType);
                    throw e;
                }
            }
        }
        if (outputType != null)
            returned.put(Manager.XP_TYPE.toString(), outputType.toString());
        return ImmutableList.of(returned).iterator();
    }

    @Override
    public String toString() {
        return "merge";
    }
}
