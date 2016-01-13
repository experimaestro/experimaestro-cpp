package net.bpiwowar.xpm.utils.introspection;

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

import java.util.HashMap;

/**
 * Created by bpiwowar on 3/10/14.
 */
public class FieldInfo implements AnnotatedElement {
    HashMap<String, AnnotationInfo> annotations = new HashMap<>();
    private String name;
    private ClassInfo type;

    public FieldInfo(String name, ClassInfo type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public <T> AnnotationInfo<T> getAnnotationInfo(Class<T> annotationClass) {
        return annotations.get(annotationClass.getName());
    }

    public ClassInfo getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
