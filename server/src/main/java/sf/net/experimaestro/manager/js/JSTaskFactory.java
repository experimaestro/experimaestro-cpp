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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.scheduler.Commands;
import sf.net.experimaestro.utils.JSNamespaceContext;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.xpath.XPathExpressionException;

/**
 * A task factory
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSTaskFactory extends JSBaseObject implements Wrapper {

    final static private Logger LOGGER = Logger.getLogger();
    TaskFactory factory;

    @JSFunction
    public JSTaskFactory(Scriptable scope, NativeObject jsObject,
                         Repository repository) throws ValueMismatchException {
        this(getQName(scope, jsObject, "id", false), scope, jsObject, repository);
    }

    @JSFunction
    public JSTaskFactory(QName qname, Scriptable scope, NativeObject jsObject,
                         Repository repository) throws ValueMismatchException {
        factory = new TaskFactoryJavascript(qname, scope, jsObject, repository);
    }

    @JSFunction
    public JSTaskFactory(TaskFactory factory) {
        this.factory = factory;
    }

    static QName getQName(Scriptable scope, NativeObject jsObject, String key, boolean allowNull) {
        Object o = JSUtils.get(scope, key, jsObject, allowNull);
        if (o == null)
            return null;

        if (o instanceof QName)
            return (QName) o;
        else if (o instanceof String) {
            return QName.parse(o.toString(), new JSNamespaceContext(scope));
        }

        throw new XPMRhinoException("Cannot transform type %s into QName", o.getClass());
    }

    @JSFunction("create")
    public JSTaskWrapper create() {
        return new JSTaskWrapper(factory.create(), xpm());
    }

    @JSFunction("commands")
    public Commands commands(JsonObject json) {
        return factory.commands(xpm().getScheduler(), json, xpm()._simulate);
    }

    @JSFunction(value = "run", scope = true)
    public Object run(Context context, Scriptable scope, NativeObject object) throws XPathExpressionException {
        return plan(context, scope, object).run(context, scope);
    }

    @JSHelp("Creates a plan from this task")
    @JSFunction(value = "plan", scope = true)
    public JSPlan plan(Context cx, Scriptable scope, NativeObject object) throws XPathExpressionException {
        return newObject(cx, scope, JSPlan.class, this, object);
    }

    @JSHelp("Creates a plan from this task")
    @JSFunction(value = "plan", scope = true)
    public JSPlan plan(Context cx, Scriptable scope) {
        return newObject(cx, scope, JSPlan.class);
    }

    @JSFunction(value = "simulate", scope = true)
    public Object simulate(Context context, Scriptable scope, NativeObject parameters) throws Exception {
        final JSPlan plan = plan(context, scope, parameters);
        return plan.simulate(context, scope);
    }

    @Override
    public TaskFactory unwrap() {
        return factory;
    }


}
