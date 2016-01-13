package sf.net.experimaestro.utils;

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

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/**
 * Defines the default exclusion strategy in XPM
 */
public class GsonExclusionStrategy implements ExclusionStrategy {
    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        final GsonSerialization annotation = f.getAnnotation(GsonSerialization.class);
        return (annotation != null && !annotation.serialize());
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }
}
