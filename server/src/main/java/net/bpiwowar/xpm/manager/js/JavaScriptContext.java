package net.bpiwowar.xpm.manager.js;

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
import org.mozilla.javascript.*;
import net.bpiwowar.xpm.exceptions.XPMRhinoException;
import net.bpiwowar.xpm.manager.TypeName;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.scripting.LanguageContext;
import net.bpiwowar.xpm.manager.scripting.ScriptLocation;
import net.bpiwowar.xpm.manager.scripting.ScriptingReference;
import net.bpiwowar.xpm.manager.scripting.Wrapper;

import javax.xml.namespace.NamespaceContext;
import java.util.function.BiFunction;

/**
 * The JavaScript context when calling a function
 */
public class JavaScriptContext extends LanguageContext {
    static BiFunction<JavaScriptContext, Object, Object> toJavaFunction;

    static {
        toJavaFunction = (jcx, value) -> {
            if (value == null) {
                return null;
            }

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

            if (value instanceof net.bpiwowar.xpm.manager.scripting.Wrapper) {
                return ((net.bpiwowar.xpm.manager.scripting.Wrapper)value).unwrap();
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
    public TypeName qname(Object value) {
        if (value instanceof Wrapper) {
            value = ((Wrapper) value).unwrap();
        }

        if (value instanceof TypeName) {
            return (TypeName) value;
        }

        return TypeName.parse(value.toString(), getNamespaceContext());
    }

    @Override
    public Object toJava(Object value) {
        return toJavaFunction.apply(this, value);
    }

    @Override
    public ScriptLocation getScriptLocation() {
        final XPMRhinoException rhinoException = new XPMRhinoException();
        final ScriptStackElement[] scriptStack = rhinoException.getScriptStack();
        if (scriptStack.length == 0) {
            return new ScriptLocation();
        }
        return new ScriptLocation(scriptStack[0].fileName, scriptStack[0].lineNumber);
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
