package sf.net.experimaestro.manager.python;

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

import sf.net.experimaestro.exceptions.XPMScriptRuntimeException;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.scripting.LanguageContext;
import sf.net.experimaestro.manager.scripting.ScriptLocation;
import sf.net.experimaestro.manager.scripting.ScriptingReference;
import sf.net.experimaestro.manager.scripting.Wrapper;

import javax.xml.namespace.NamespaceContext;
import java.util.function.BiFunction;

/**
 * The JavaScript context when calling a function
 */
public class PythonContext extends LanguageContext {
    static BiFunction<PythonContext, Object, Object> toJavaFunction;

    static {
        toJavaFunction = (jcx, value) -> {
            if (value == null) {
                return null;
            }

            if (value instanceof PythonObject) {
                return ((PythonObject)value).object;
            }


            if (value instanceof ScriptingReference) {
                value = ((ScriptingReference) value).get(jcx);
            }

            if (value instanceof sf.net.experimaestro.manager.scripting.Wrapper) {
                return ((sf.net.experimaestro.manager.scripting.Wrapper) value).unwrap();
            }

            if (value.getClass().isArray()) {
                Object[] array = (Object[]) value;
                Object[] newArray = new Object[array.length];
                for (int i = 0; i < newArray.length; ++i) {
                    newArray[i] = toJavaFunction.apply(jcx, array[i]);
                }
                return newArray;
            }

            return value;
        };
    }

    public PythonContext() {
        super();
    }

    @Override
    public Json toJSON(Object object) {
        return Json.toJSON(this, object);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return new PythonNamespaceContext(this);
    }

    @Override
    public RuntimeException runtimeException(Exception e, String format, Object... objects) {
        return new XPMScriptRuntimeException(e, format, objects);
    }

    @Override
    public QName qname(Object value) {
        if (value instanceof Wrapper) {
            value = ((Wrapper) value).unwrap();
        }

        if (value instanceof QName) {
            return (QName) value;
        }

        return QName.parse(value.toString(), getNamespaceContext());
    }

    @Override
    public Object toJava(Object value) {
        return toJavaFunction.apply(this, value);
    }

    @Override
    public ScriptLocation getScriptLocation() {
        throw new org.apache.commons.lang.NotImplementedException();
//        return new ScriptLocation(scriptStack[0].fileName, scriptStack[0].lineNumber);
    }

}
