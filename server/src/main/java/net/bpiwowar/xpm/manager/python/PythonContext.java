package net.bpiwowar.xpm.manager.python;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.python.core.*;
import net.bpiwowar.xpm.exceptions.XPMScriptRuntimeException;
import net.bpiwowar.xpm.manager.QName;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.scripting.*;

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
                Object object = ((PythonObject) value).object;
                if (object instanceof WrapperObject) {
                    object = ((WrapperObject)object).unwrap();
                }
                return object;
            }

            if (value instanceof PyDictionary) {
                final com.google.common.base.Function f = x -> toJavaFunction.apply(jcx, x);
                return Maps.transformValues((PyDictionary) value, f);
            }

            if (value instanceof PyList) {
                final com.google.common.base.Function f = x -> toJavaFunction.apply(jcx, x);
                return Lists.transform((PyList) value, f);
            }

            if (value instanceof ScriptingReference) {
                value = ((ScriptingReference) value).get(jcx);
            }

            if (value instanceof net.bpiwowar.xpm.manager.scripting.Wrapper) {
                return ((net.bpiwowar.xpm.manager.scripting.Wrapper) value).unwrap();
            }

            if (value.getClass().isArray()) {
                Object[] array = (Object[]) value;
                Object[] newArray = new Object[array.length];
                for (int i = 0; i < newArray.length; ++i) {
                    newArray[i] = toJavaFunction.apply(jcx, array[i]);
                }
                return newArray;
            }

            if (value instanceof PyBoolean) {
                return ((PyBoolean)value).getBooleanValue();
            }
            if (value instanceof PyLong) {
                return ((PyLong)value).getValue();
            }
            if (value instanceof PyInteger) {
                return ((PyInteger)value).getValue();
            }
            if (value instanceof PyFloat) {
                return ((PyFloat)value).getValue();
            }
            if (value instanceof PyString) {
                return ((PyString)value).getString();
            }

            return value;
        };
    }

    public PythonContext() {
        super();
    }

    @Override
    public Json toJSON(Object object) {
        return PythonUtils.toJSON(object);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return new PythonNamespaceContext();
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
        ThreadState threadState = Py.getThreadState();
        return new ScriptLocation(threadState.frame.f_code.co_filename , threadState.frame.f_lineno);
    }

}