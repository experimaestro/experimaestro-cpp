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
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.Input;
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
    private Type outputType;

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
                        NativeObject jsFactory, Function runFunction, Type outputType) {
        super(taskFactory, jsScope);
        this.xpm = xpm;
        this.jsFactory = jsFactory;
        this.runFunction = runFunction;
        this.outputType = outputType;
    }

    @Override
    protected void init(Task _other) {
        JSDirectTask other = (JSDirectTask) _other;
        super.init(other);
        jsFactory = other.jsFactory;
        runFunction = other.runFunction;
        xpm = other.xpm;
        outputType = other.outputType;
    }

    @Override
    public Document jsrun(boolean simulate) {
        LOGGER.debug("[Running] task: %s", factory.getId());

        final Context cx = Context.getCurrentContext();

        // Get the inputs
        Document result = null;

        if (runFunction != null) {
            // We have a run function
            Scriptable jsXML = cx.newObject(jsScope, "Object", new Object[]{});
            for (Entry<String, Value> entry : values.entrySet()) {
                Document input = entry.getValue().get();
                Object xml = input == null ? Scriptable.NOT_FOUND : new JSNode(input);
                jsXML.put(entry.getKey(), jsXML, xml);
            }

            boolean old = xpm.simulate;
            xpm.simulate = simulate | xpm.simulate;
            final Object returned = runFunction.call(cx, jsScope, jsFactory,
                    new Object[]{jsXML});
            xpm.simulate = old;
            LOGGER.debug("Returned %s", returned);
            if (returned == Undefined.instance || returned == null)
                throw new ExperimaestroRuntimeException(
                        "Undefined returned by the function run of task [%s]",
                        factory.getId());

            result = JSUtils.toDocument(jsScope, returned);
        } else {
            // We just copy the inputs as an output

            Document document = XMLUtils.newDocument();

            Element root;
            if (outputType == null)
                root = document.createElementNS(Manager.EXPERIMAESTRO_NS, "array");
            else
                root = document.createElementNS(outputType.getNamespaceURI(), outputType.getLocalPart());

            document.appendChild(root);

            // Loop over non null inputs
            for (Entry<String, Value> entry : values.entrySet()) {
                Value value = entry.getValue();
                if (value != null) {
                    final Node doc = value.get();


                    if (doc != null) {
                        Element element = XMLUtils.getRootElement(doc);
                        element = (Element) element.cloneNode(true);
                        document.adoptNode(element);

                        Input input = value.getInput();
                        if (input != null && input.getType() instanceof ValueType) {
                            Element newRoot = document.createElementNS(input.getNamespace(), entry.getKey());

                            NamedNodeMap attributes = element.getAttributes();
                            for (int i = 0; i < attributes.getLength(); i++) {
                                Attr newAttr = (Attr) (attributes.item(i).cloneNode(false));
                                newRoot.setAttributeNodeNS(newAttr);
                            }

                            for (Node child : XMLUtils.iterable(element.getChildNodes())) {
                                newRoot.appendChild(child);
                            }

                            element = newRoot;
                        }


                        element.setAttributeNS(Manager.EXPERIMAESTRO_NS, "name",
                                entry.getKey());
                        root.appendChild(element);
                    }
                }
            }

            result = document;

        }


        LOGGER.debug("[/Running] task: %s", factory.getId());

        return result;
    }
}
