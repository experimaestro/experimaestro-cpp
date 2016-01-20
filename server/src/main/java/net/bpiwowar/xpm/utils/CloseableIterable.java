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

import net.bpiwowar.xpm.exceptions.CloseException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;

import java.util.function.Consumer;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public interface CloseableIterable<T> extends Iterable<T>, AutoCloseable {
    @Override
    void close() throws CloseException;

    @Override
    default void forEach(Consumer<? super T> action) {
        try {
            Iterable.super.forEach(action);
        } finally {
            try {
                close();
            } catch (CloseException e) {
                throw new XPMRuntimeException(e);
            }
        }
    }
}
