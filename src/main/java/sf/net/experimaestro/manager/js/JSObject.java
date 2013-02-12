/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.*;

import java.lang.reflect.InvocationTargetException;

/**
 * Base class for all JS objects
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 27/11/12
 */
public abstract class JSObject implements JSConstructable {

    /**
     * Returns the class name
     */
    static String getClassName(Class<?> aClass) {
        assert aClass.getSimpleName().startsWith("JS");
        return aClass.getSimpleName().substring(2);
    }


    /**
     * Defines a new class.
     * <p/>
     * Used in order to plug our class constructor {@linkplain MyNativeJavaClass}
     * if the object is a {@linkplain JSObject} or a {@linkplain JSBaseObject}
     *
     * @param scope
     * @param aClass
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    public static void defineClass(Scriptable scope, Class<? extends Scriptable> aClass) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // If not a JSObject descendent, we handle this with standard JS procedure
        if (JSConstructable.class.isAssignableFrom(aClass)) {
            // Use our own constructor
            final String name = getClassName(aClass);
            scope = ScriptableObject.getTopLevelScope(scope);
            final NativeJavaClass nativeJavaClass = new MyNativeJavaClass(scope, aClass);
            scope.put(name, scope, nativeJavaClass);
        } else {
            ScriptableObject.defineClass(scope, aClass);
        }
    }


    private static class MyNativeJavaClass extends NativeJavaClass {
        public MyNativeJavaClass(Scriptable scriptable, Class<? extends Scriptable> aClass) {
            super(scriptable, aClass);
        }

        @Override
        public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
            return super.construct(cx, scope, args);
        }
    }

    /**
     * The Experimaestro wrap factory to handle special cases
     */
    static public class XPMWrapFactory extends WrapFactory {
        public final static XPMWrapFactory INSTANCE = new XPMWrapFactory();

        private XPMWrapFactory() {
        }


        @Override
        public Object wrap(Context cx, Scriptable scope, Object obj, Class<?> staticType) {
            if (obj instanceof JSPlan.JSPlanRef) {
                return new JSPlan(((JSPlan.JSPlanRef) obj).getPlan(), ((JSPlan.JSPlanRef) obj).xpath);
            }

            return super.wrap(cx, scope, obj, staticType);
        }

        @Override
        public Scriptable wrapNewObject(Context cx, Scriptable scope, Object obj) {
            if (obj instanceof JSObject)
                return new MyNativeJavaObject(scope, obj, obj.getClass(), false);


            return super.wrapNewObject(cx, scope, obj);
        }

    }

    static private class MyNativeJavaObject extends NativeJavaObject {
        private MyNativeJavaObject(Scriptable scope, Object javaObject, Class<?> staticType, boolean isAdapter) {
            super(scope, javaObject, staticType, isAdapter);
        }

        @Override
        public String getClassName() {
            return JSObject.getClassName(this.staticType);
        }
    }
}
