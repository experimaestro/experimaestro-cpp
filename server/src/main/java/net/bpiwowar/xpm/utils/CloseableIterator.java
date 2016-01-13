package net.bpiwowar.xpm.utils;

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

import com.google.common.collect.AbstractIterator;
import net.bpiwowar.xpm.exceptions.CloseException;
import net.bpiwowar.xpm.scheduler.Resource;

import java.util.Iterator;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 23/1/13
 */
public abstract class CloseableIterator<T> extends AbstractIterator<T> implements AutoCloseable {
    static public CloseableIterator<Resource> of(final Iterator<Resource> iterator) {
        return new CloseableIterator<Resource>() {
            @Override
            public void close() throws CloseException {
            }

            @Override
            protected Resource computeNext() {
                if (iterator.hasNext())
                    return iterator.next();
                return endOfData();
            }
        };
    }

    @Override
    public abstract void close() throws CloseException;
}
