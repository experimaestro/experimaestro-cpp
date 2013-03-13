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

package sf.net.experimaestro.manager;

import org.w3c.dom.Element;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.utils.XMLUtils;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 18/2/13
 */
public class ArrayType extends Type {
    final private static QName QNAME = new QName(Manager.EXPERIMAESTRO_NS, "array");
    private final Type innerType;

    public ArrayType(Type innerType) {
        super(QNAME);
        this.innerType = innerType;
    }

    @Override
    public void validate(Element element) throws ValueMismatchException {
        if (!QNAME.sameQName(element))
            throw new ExperimaestroRuntimeException("Expected %s and got %s", QNAME,
                    new QName(element));

        for (Element child : XMLUtils.childElements(element)) {
            innerType.validate(child);
        }
    }


}
