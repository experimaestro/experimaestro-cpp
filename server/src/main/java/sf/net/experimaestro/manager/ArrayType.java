package sf.net.experimaestro.manager;

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

import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonArray;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 18/2/13
 */
public class ArrayType extends Type {
    final private static QName QNAME = new QName(Constants.EXPERIMAESTRO_NS, "array");
    private final Type innerType;

    public ArrayType(Type innerType) {
        super(QNAME);
        this.innerType = innerType;
    }

    @Override
    public void validate(Json element) throws ValueMismatchException {
        // Check if this is an array
        if (!(element instanceof JsonArray))
            throw new XPMRuntimeException("Expected an array and got " + element.getClass());

        // Check every element
        JsonArray array = (JsonArray)element;
        int i = 0;
        for(Json json: array) {
            try {
                innerType.validate(json);
                ++i;
            } catch(ValueMismatchException e) {
                e.addContext("While validating element %d of array of type %s", i, innerType);
                throw e;
            }
        }
    }


}
