package sf.net.experimaestro.manager.js.object;

import org.apache.commons.vfs2.FileObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.js.*;
import sf.net.experimaestro.scheduler.*;
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
     * @return a valid {@linkplain sf.net.experimaestro.scheduler.CommandArgument} object
     */
    public static Command getCommand(Object jsargs) {
        final Command command = new Command();

        if (jsargs instanceof List) {
            NativeArray array = ((NativeArray) jsargs);

            for (Object _object : array) {
                final CommandArgument argument = new CommandArgument();
                Object object = JSUtils.unwrap(_object);
                StringBuilder sb = new StringBuilder();

                // XML argument (deprecated -- too many problems with E4X!)
                if (JSUtils.isXML(object)) {

                    // Walk through
                    for (Node child : xmlAsList(JSUtils.toDOM(array, object)))
                        argumentWalkThrough(sb, argument, child);

                } else if (object instanceof Pipe) {
                    command.add(AbstractCommandArgument.PIPE);
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
    private static void argumentWalkThrough(Scriptable scope, StringBuilder sb, CommandArgument command, Object object) {

        if (object == null)
            throw new IllegalArgumentException(String.format("Null argument in command line"));

        if (object instanceof JSFileObject)
            object = ((JSFileObject) object).getFile();

        if (object instanceof FileObject) {
            if (sb.length() > 0) {
                command.add(sb.toString());
                sb.delete(0, sb.length());
            }
            command.add(new CommandComponent.Path((FileObject) object));
        } else if (object instanceof NativeArray) {
            for (Object child : (NativeArray) object)
                argumentWalkThrough(scope, sb, command, JSUtils.unwrap(child));
        } else if (JSUtils.isXML(object)) {
            final Object node = JSUtils.toDOM(scope, object);
            for (Node child : xmlAsList(node))
                argumentWalkThrough(sb, command, child);
        } else if (object instanceof JSParameterFile) {
            final JSParameterFile pFile = (JSParameterFile) object;
            command.add(new CommandComponent.ParameterFile(pFile.getKey(), pFile.getValue()));
        } else {
            sb.append(JSUtils.toString(object));
        }
    }

    /**
     * Walk through a node hierarchy to build a command argument
     *
     * @param sb
     * @param argument
     * @param node
     */
    private static void argumentWalkThrough(StringBuilder sb, CommandArgument argument, Node node) {
        switch (node.getNodeType()) {
            case Node.TEXT_NODE:
                sb.append(node.getTextContent());
                break;

            case Node.ATTRIBUTE_NODE:
                if (Manager.XP_PATH.sameQName(node)) {
                    argument.add(new CommandComponent.Path(node.getNodeValue()));
                } else
                    sb.append(node.getTextContent());
                break;

            case Node.DOCUMENT_NODE:
                argumentWalkThrough(sb, argument, ((Document) node).getDocumentElement());
                break;

            case Node.ELEMENT_NODE:
                Element element = (Element) node;
                if (XMLUtils.is(Manager.XP_PATH, element)) {
                    if (sb.length() > 0) {
                        argument.add(sb.toString());
                        sb.delete(0, sb.length());
                    }
                    argument.add(new CommandComponent.Path(element.getTextContent()));
                } else {
                    for (Node child : XMLUtils.children(node))
                        argumentWalkThrough(sb, argument, child);
                }

                break;
            default:
                throw new ExperimaestroRuntimeException("Unhandled command XML node  " + node.toString());
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
