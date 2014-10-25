package sf.net.experimaestro.manager.js;

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

import com.google.common.collect.Iterables;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Created by bpiwowar on 10/9/14.
 */
public class ConstructorFunction extends GenericFunction {
    ArrayList<Constructor<?>> constructors = new ArrayList<>();
    private String className;

    public ConstructorFunction(String className, ArrayList<Constructor<?>> constructors) {
        this.className = className;
        this.constructors = constructors;
    }

    @Override
    protected String getName() {
        return "new " + className;
    }

    @Override
    protected Iterable<ConstructorDeclaration> declarations() {
        return Iterables.transform(constructors, ConstructorDeclaration::new);
    }

    static public class ConstructorDeclaration extends Declaration<Constructor> {
        Constructor<?> constructor;

        public ConstructorDeclaration(Constructor constructor) {
            super(constructor);
            this.constructor = constructor;
        }

        @Override
        public Object invoke(Object[] transformedArgs) throws IllegalAccessException, InvocationTargetException, InstantiationException {
            return constructor.newInstance(transformedArgs);
        }

    }
}
