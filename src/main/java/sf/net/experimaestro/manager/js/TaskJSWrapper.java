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

import com.google.common.collect.ImmutableList;
import org.mozilla.javascript.*;
import org.mozilla.javascript.annotations.JSFunction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Task as seen by JavaScript
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
                             Object[] args, Function funObj) throws Exception {
        List<Document> result = run(thisObj, args, true);
        assert result.size() == 1;
        return wrap(result, thisObj).get(0);

    }

    private static List<Object> wrap(List<Document> result, Scriptable scope) {
        ArrayList list = new ArrayList();
        for (Document document : result) {
            list.add(JSUtils.domToE4X(document, Context.getCurrentContext(), scope));
        }
        return list;
    }

    @JSFunction(value = "run_plan")
    static public Object run_plan(Context cx, Scriptable thisObj,
                                  Object[] args, Function funObj) throws Exception {
        return wrap(run(thisObj, args, false), thisObj);
    }


    private static List<Document> run(Scriptable thisObj, Object[] args, boolean singlePlan) throws Exception {
        final TaskJSWrapper wrapper = (TaskJSWrapper) thisObj;

        if (args.length == 0)
            return ImmutableList.of(wrapper.getTask().run());

        if (args.length == 1) {
            final String planString = Context.toString(args[0]);
            final List<Document> documents = wrapper.getTask().runPlan(planString, singlePlan, new JSScriptRunner(thisObj));
            return documents;
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
     * Set a parameter
     *
     * @param _id   The ID of the parameter
     * @param value
     */
    public void jsFunction_setParameter(String _id, Scriptable value) {
        DotName id = DotName.parse(_id);
        LOGGER.debug("Setting input [%s] to [%s] of type %s", _id, value,
                value.getClass());

        if (value == Scriptable.NOT_FOUND)
            getTask().setParameter(id, (Document) null);
        else if (value instanceof Element) {
            LOGGER.debug("Value is an XML element");
            Document document = XMLUtils.newDocument();
            Node node = ((Element) value).cloneNode(true);
            document.adoptNode(node);
            document.appendChild(node);
            getTask().setParameter(id, document);
        } else if (JSUtils.isXML(value)) {
            LOGGER.debug("Value is JS XML");
            Document document = XMLUtils.newDocument();
            Node node = ((Node) JSUtils.toDOM(value)).cloneNode(true);
            if (node.getNodeType() == Node.ATTRIBUTE_NODE)
                getTask().setParameter(id, node.getNodeValue());
            else {
                document.adoptNode(node);
                document.appendChild(node);
                getTask().setParameter(id, document);
            }
        } else {
            LOGGER.debug("Value will be converted to string [%s/%s]",
                    value.getClassName(), value.getClass());
            getTask().setParameter(id, JSUtils.toString(value));
        }
    }


    public Task getTask() {
        return task;
    }
}