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

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.vfs2.FileObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.manager.Type;
import sf.net.experimaestro.manager.Value;
import sf.net.experimaestro.manager.ValueType;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

import java.util.Map.Entry;

public class JSDirectTask extends JSAbstractTask {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The run function
     */
    private Function runFunction;

    /**
     * The object?
     */
    private NativeObject jsFactory;

    /**
     * The XPM object
     */
    private XPMObject xpm;

    public JSDirectTask() {
    }

    public JSDirectTask(XPMObject xpm, TaskFactory taskFactory, Scriptable jsScope,
                        NativeObject jsFactory, Function runFunction) {
        super(taskFactory, jsScope);
        this.xpm = xpm;
        this.jsFactory = jsFactory;
        this.runFunction = runFunction;

    }

    @Override
    public Document jsrun() {
        LOGGER.debug("[Running] task: %s", factory.getId());

        final Context cx = Context.getCurrentContext();

        // Get the inputs
        Document result = null;

        if (runFunction != null) {
            // We have a run function
            Scriptable jsDirect = cx.newObject(jsScope, "Object", new Object[]{});
            Scriptable jsXML = cx.newObject(jsScope, "Object", new Object[]{});
            getJSInputs(cx, jsXML, jsDirect);
            final Object returned = runFunction.call(cx, jsScope, jsFactory,
                    new Object[]{jsXML, jsDirect});
            LOGGER.debug("Returned %s", returned);
            if (returned == Undefined.instance || returned == null)
                throw new ExperimaestroRuntimeException(
                        "Undefined returned by the function run of task [%s]",
                        factory.getId());

            result = JSUtils.toDocument(jsScope, returned);
        } else {
            // We just copy the inputs as an output

            Document document = XMLUtils.newDocument();

            Element root = document.createElementNS(Manager.EXPERIMAESTRO_NS,
                    "array");
            document.appendChild(root);

            // Loop over non null inputs
            for (Entry<String, Value> entry : values.entrySet()) {
                Value value = entry.getValue();
                if (value != null) {
                    final Node doc = value.get();

                    if (doc != null) {
                        Element element = XMLUtils.getRootElement(doc);
                        element.setAttributeNS(Manager.EXPERIMAESTRO_NS, "name",
                                entry.getKey());
                        document.adoptNode(element);
                        root.appendChild(element);
                    }
                }
            }

            result = document;

        }


        LOGGER.debug("[/Running] task: %s", factory.getId());

        return result;
    }

    @Override
    protected void init(Task _other) {
        JSDirectTask other = (JSDirectTask) _other;
        super.init(other);
        jsFactory = other.jsFactory;
        runFunction = other.runFunction;
    }

    protected void getJSInputs(Context cx, Scriptable jsE4X, Scriptable jsUnwrapped) {
        for (Entry<String, Value> entry : values.entrySet()) {
            String id = entry.getKey();
            Value value = entry.getValue();
            JSNode xml = new JSNode(value.get());
            jsE4X.put(id, jsE4X, xml);

            Type type = value.getType();
            if (type instanceof ValueType) {
                Object object = ((ValueType) type).unwrap(value.get());
                if (object instanceof FileObject)
                    object = new JSFileObject(xpm, (FileObject) object);
                else if (!instanceOf(object, String.class, Float.class, Double.class, Integer.class, Long.class, Character.class))
                    throw new NotImplementedException(String.format("Ooops. Don't know how to handle %s for JS", object.getClass()));
                jsUnwrapped.put(id, jsUnwrapped, object);
            } else
                jsUnwrapped.put(id, jsUnwrapped, xml);
        }
    }

    private boolean instanceOf(Object object, Class<?>... classes) {
        for (Class<?> klass : classes)
            if (klass.isInstance(object))
                return true;
        return false;
    }


}
