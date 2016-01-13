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

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;

/**
 * Created by bpiwowar on 3/10/14.
 */
public interface AnnotatedElement {
    default <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        final AnnotationInfo info = getAnnotationInfo(annotationClass);

        if (info == null)
            return null;

        return (T) Proxy.newProxyInstance(
                annotationClass.getClassLoader(),
                new Class[]{annotationClass},
                (proxy, method, args) -> {
                    // Get value
                    final String name = method.getName();
                    final Object o = info.content.get(name);
                    if (o != null) {
                        return o;
                    }

                    // Get default value
                    try {
                        return annotationClass.getMethod(name).getDefaultValue();
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    <T> AnnotationInfo<T> getAnnotationInfo(Class<T> annotationClass);
}
