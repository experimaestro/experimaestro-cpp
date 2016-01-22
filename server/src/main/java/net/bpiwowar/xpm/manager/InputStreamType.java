package net.bpiwowar.xpm.manager;

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

import net.bpiwowar.xpm.exceptions.ValueMismatchException;
import net.bpiwowar.xpm.manager.json.Json;

/**
 * A type that is corresponds to an input stream
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class InputStreamType extends Type {
    final private static TypeName QNAME = new TypeName(Constants.EXPERIMAESTRO_NS, "input-stream");
    private final TypeName innerType;

    public InputStreamType(TypeName innerType) {
        super(QNAME);
        this.innerType = innerType;
    }

    @Override
    public void validate(Json element) throws ValueMismatchException {
        if (!(element instanceof JsonTask)) {
            throw new ValueMismatchException("Expected a runnable task argument");
        }
    }


}
