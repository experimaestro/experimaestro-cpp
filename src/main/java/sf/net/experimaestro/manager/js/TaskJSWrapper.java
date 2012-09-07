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
import org.mozilla.javascript.annotations.JSFunction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.plan.ParseException;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

import java.util.List;

/**
 * Task factory as seen by JavaScript
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TaskJSWrapper extends ScriptableObject {
    private static final long serialVersionUID = 1L;
    final static private Logger LOGGER = Logger.getLogger();

    private Task task;

    public TaskJSWrapper() {
    }

    public void jsConstructor(Scriptable task) {
        if (task != null)
            this.task = ((Task) ((NativeJavaObject) task).unwrap());
    }

    @Override
    public String getClassName() {
        return "XPMTask";
    }

    // ---- JavaScript functions ----

    /**
     * Run
     */
    @JSFunction(value = "run")
    static public Object run(Context cx, Scriptable thisObj,
                      Object[] args, Function funObj) throws ParseException {
        final TaskJSWrapper wrapper = (TaskJSWrapper) thisObj;

        if (args.length == 0)
            return JSUtils.domToE4X(wrapper.getTask().run(), Context.getCurrentContext(),
                    wrapper);

        if (args.length == 1) {
            final String planString = Context.toString(args[0]);
            final List<Document> documents = wrapper.getTask().runPlan(planString, true);
            return JSUtils.domToE4X(documents.get(0), Context.getCurrentContext(),
                    wrapper);
        }

        throw new IllegalArgumentException("run() method expects zero or one argument");
    }

    /**
     * Just a short hand for setParameter
     *
     * @param _id
     * @param value
     */
    public void jsFunction_set(String _id, Scriptable value) {
        jsFunction_setParameter(_id, value);
    }

    /**
     * Run an experimental plan
     *
     * @param plan
     * @throws ParseException
     */
    public List<Document> jsFunction_run_plan(String plan) throws ParseException {
        final List<Document> documents = task.runPlan(plan, false);
        return documents;
    }

    /**
     * Set a parameter
     *
     * @param _id The ID of the parameter
     * @param value
     */
    public void jsFunction_setParameter(String _id, Scriptable value) {
        DotName id = DotName.parse(_id);
        LOGGER.info("Setting input [%s] to [%s] of type %s", _id, value,
                value.getClass());

        if (value == Scriptable.NOT_FOUND)
            getTask().setParameter(id, (Document) null);
        else if (value instanceof Element) {
            LOGGER.info("Value is an XML element");
            Document document = XMLUtils.newDocument();
            Node node = ((Element) value).cloneNode(true);
            document.adoptNode(node);
            document.appendChild(node);
            getTask().setParameter(id, document);
        } else if (JSUtils.isXML(value)) {
            LOGGER.info("Value is XML");
            Document document = XMLUtils.newDocument();
            Node node = ((Element) JSUtils.toDOM(value)).cloneNode(true);
            document.adoptNode(node);
            document.appendChild(node);
            getTask().setParameter(id, document);
        } else {
            LOGGER.info("Value will be converted to string [%s/%s]",
                    value.getClassName(), value.getClass());
            getTask().setParameter(id, (String) value.toString());
        }
    }

    public Task getTask() {
        return task;
    }
}