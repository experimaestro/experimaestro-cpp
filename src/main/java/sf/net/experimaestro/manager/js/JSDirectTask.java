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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.Value;
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

    public JSDirectTask() {
    }

    public JSDirectTask(JSTaskFactory jsTaskFactory, Scriptable jsScope,
                        NativeObject jsFactory, Function runFunction) {
        super(jsTaskFactory, jsScope);
        this.jsFactory = jsFactory;
        this.runFunction = runFunction;

    }

    @Override
    public Object jsrun(boolean convertToE4X) {
        LOGGER.debug("[Running] task: %s", factory.getId());

        final Context cx = Context.getCurrentContext();

        // Get the inputs
        Object result = null;

        if (runFunction != null) {
            // We have a run function
            Scriptable jsInputs = getJSInputs();
            final Object returned = runFunction.call(cx, jsScope, jsFactory,
                    new Object[]{jsInputs});
            LOGGER.debug("Returned %s", returned);
            if (returned == Undefined.instance)
                throw new ExperimaestroRuntimeException(
                        "Undefined returned by the function run");

            result = (Scriptable) returned;
        } else {
            // We just copy the inputs as an output

            Document document = XMLUtils.newDocument();
            result = document;

            Element root = document.createElementNS(Manager.EXPERIMAESTRO_NS,
                    "outputs");
            document.appendChild(root);

            // Loop over non null inputs
            for (Entry<String, Value> entry : values.entrySet()) {
                Value value = entry.getValue();
                if (value != null) {
//                    Element outputs = document.createElementNS(
//                            Manager.EXPERIMAESTRO_NS, "outputs");
//                    root.appendChild(outputs);
                    final Document doc = value.get();

                    if (doc != null) {
                        Element element = (Element) doc.getDocumentElement().cloneNode(true);
                        element.setAttributeNS(Manager.EXPERIMAESTRO_NS, "name",
                                entry.getKey());
                        document.adoptNode(element);
                        root.appendChild(element);
                    }
                }
            }

            if (convertToE4X)
                result = JSUtils.domToE4X(document, cx, jsScope);
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
}
