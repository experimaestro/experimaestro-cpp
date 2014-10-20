package sf.net.experimaestro.manager.js.object;

import org.apache.commons.vfs2.FileObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.js.*;
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
    /** The underlying command */
    Command command;

    @JSFunction
    public JSCommand(List array) {
        command = getCommand(array);
    }

    @JSProperty("out")
    StreamReference stream;

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

        if (object instanceof JSFileObject)
            object = ((JSFileObject) object).getFile();

        if (object instanceof FileObject) {
            if (sb.length() > 0) {
                command.add(sb.toString());
                sb.delete(0, sb.length());
            }
            command.add(new Command.Path((FileObject) object));
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
