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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Ref;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.scripting.LanguageContext;
import sf.net.experimaestro.manager.scripting.ScriptingReference;
import sf.net.experimaestro.manager.scripting.Wrapper;
import sf.net.experimaestro.utils.JSNamespaceContext;

import javax.xml.namespace.NamespaceContext;
import java.util.function.BiFunction;

/**
 * The JavaScript context when calling a function
 */
public class JavaScriptContext extends LanguageContext {
    static BiFunction<JavaScriptContext, Object, Object> toJavaFunction;

    static {
        toJavaFunction = (jcx, value) -> {
            if (value instanceof NativeObject) {
                final com.google.common.base.Function f = x -> toJavaFunction.apply(jcx, x);
                return Maps.transformValues((NativeObject) value, f);
            }

            if (value instanceof NativeArray) {
                final com.google.common.base.Function f = x -> toJavaFunction.apply(jcx, x);
                return Lists.transform((NativeArray) value,  f);
            }

            if (value instanceof ConsString) {
                return value.toString();
            }

            if (value instanceof ScriptingReference) {
                value = ((ScriptingReference) value).get(jcx);
            }

            if (value instanceof sf.net.experimaestro.manager.scripting.Wrapper) {
                return ((sf.net.experimaestro.manager.scripting.Wrapper)value).unwrap();
            }

            if (value instanceof Ref) {
                value = ((Ref)value).get(jcx.context);
            }

            if (value == Undefined.instance) {
                return null;
            }

            if (value.getClass().isArray()) {
                Object[] array = (Object[]) value;
                Object[] newArray = new Object[array.length];
                for(int i = 0; i < newArray.length; ++i) {
                    newArray[i] = toJavaFunction.apply(jcx, array[i]);
                }
                return newArray;
            }

            return value;
        };
    }

    private final Context context;

    private Scriptable scope;

    public JavaScriptContext(Context context, Scriptable scope) {
        super();
        this.context = context;
        this.scope = scope;
    }

    @Override
    public Json toJSON(Object object) {
        return Json.toJSON(this, object);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return new JSNamespaceContext(scope);
    }

    @Override
    public RuntimeException runtimeException(Exception e, String format, Object... objects) {
        return new XPMRhinoException(e, format, objects);
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

    public Context context() {
        return context;
    }

    public Scriptable scope() {
        return scope;
    }

    public Context getContext() {
        return context;
    }

}
