package sf.net.experimaestro.manager.js.object;

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

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sf.net.experimaestro.manager.js.*;
import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.manager.scripting.ScriptingPath;
import sf.net.experimaestro.manager.scripting.Property;
import sf.net.experimaestro.scheduler.AbstractCommand;
import sf.net.experimaestro.scheduler.Command;
import sf.net.experimaestro.scheduler.CommandComponent;
import sf.net.experimaestro.scheduler.StreamReference;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.XMLUtils;

import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

@JSObjectDescription()
public class JSCommand extends JSBaseObject implements Wrapper {
    /**
     * The underlying command
     */
    AbstractCommand command;
    @Property("out")
    StreamReference stream;

    @Expose
    public JSCommand(List array) {
        command = getCommand(array);
    }

    /**
     * Transform an array of JS objects into a command line argument object
     *
     * @param jsargs The input array
     * @return a valid {@linkplain sf.net.experimaestro.scheduler.Command} object
     */
    public static Command getCommand(Object jsargs) {
        final Command command = new Command();

        if (jsargs instanceof List) {
            NativeArray array = ((NativeArray) jsargs);

            for (Object _object : array) {
                final Command argument = new Command();
                Object object = JSUtils.unwrap(_object);
                StringBuilder sb = new StringBuilder();

                if (object instanceof CommandComponent) {
                    command.add(object);
                } else {
                    argumentWalkThrough(array, sb, argument, object);

                    if (sb.length() > 0)
                        argument.add(sb.toString());

                    command.add(argument);
                }

            }

        } else
            throw new RuntimeException(format(
                    "Cannot handle an array of type %s", jsargs.getClass()));
        return command;
    }

    /**
     * Recursive parsing of the command line
     */
    private static void argumentWalkThrough(Scriptable scope, StringBuilder sb, Command command, Object object) {

        if (object == null)
            throw new IllegalArgumentException(String.format("Null argument in command line"));

        if (object instanceof ScriptingPath)
            object = ((ScriptingPath) object).getPath();

        if (object instanceof java.nio.file.Path) {
            if (sb.length() > 0) {
                command.add(sb.toString());
                sb.delete(0, sb.length());
            }
            command.add(new Command.Path((java.nio.file.Path) object));
        } else if (object instanceof NativeArray) {
            for (Object child : (NativeArray) object)
                argumentWalkThrough(scope, sb, command, JSUtils.unwrap(child));
        } else if (object instanceof JSParameterFile) {
            final JSParameterFile pFile = (JSParameterFile) object;
            command.add(new Command.ParameterFile(pFile.getKey(), pFile.getValue()));
        } else {
            sb.append(JSUtils.toString(object));
        }
    }

    /**
     * Transforms an XML related object into a list
     *
     * @param object
     * @return
     */
    public static Iterable<? extends Node> xmlAsList(Object object) {

        if (object instanceof Node) {
            Node node = (Node) object;
            return (node.getNodeType() == Node.ELEMENT_NODE && node.getChildNodes().getLength() > 1)
                    || node.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE ?

                    XMLUtils.children(node) : Arrays.asList(node);
        }

        if (object instanceof NodeList)
            return XMLUtils.iterable((NodeList) object);

        throw new AssertionError("Cannot handle object of type " + object.getClass());
    }

    @Override
    public Object unwrap() {
        return command;
    }

}
