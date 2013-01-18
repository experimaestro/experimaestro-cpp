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

import org.mozilla.javascript.Scriptable;

/**
 * Base class for all JS objects
 *
 * TODO: change the base class to this one when possible for cleaner documentation and easier
 * implementation
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 27/11/12
 */
public  class JSBaseObject implements Scriptable {

    /**
     * Returns the class name
     */
    static String getClassName(Class<?> aClass) {
        assert aClass.getSimpleName().startsWith("JS");
        return aClass.getSimpleName().substring(2);
    }


    @Override
    public String getClassName() {
        return JSBaseObject.getClassName(this.getClass());
    }

    @Override
    public Object get(String name, Scriptable start) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object get(int index, Scriptable start) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void delete(String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void delete(int index) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Scriptable getPrototype() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setPrototype(Scriptable prototype) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Scriptable getParentScope() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setParentScope(Scriptable parent) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object[] getIds() {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
