package sf.net.experimaestro.utils;

import static java.lang.String.format;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.UniqueTag;
import org.mozilla.javascript.xmlimpl.XMLLibImpl;
import org.w3c.dom.Node;

public class JSUtils {

	/**
	 * Get an object from a scriptable
	 * @param object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public
	static <T> T get(Scriptable scope, String name, NativeObject object) {
		final Object _value = object.get(name, scope);
		if (_value == UniqueTag.NOT_FOUND)
			throw new RuntimeException(format("Could not find property '%s'",
					name));
		return (T) _value;
	}
	
	/**
	 * Transforms a DOM node to a E4X scriptable object
	 * @param node
	 * @param cx
	 * @param scope
	 * @return
	 */
	public static Scriptable domToE4X(Node node, Context cx, Scriptable scope) {
		return cx.newObject(scope, "XML", new Node[] { node });
	}
	
	/**
	 * Transform objects into an XML node
	 * @param object
	 * @return
	 */
	public static Node toDOM(Object object) {
		// It is already a DOM node
		if (object instanceof Node)
			return (Node) object;
		
		// Otherwise, use Rhino implementation
		return XMLLibImpl.toDomNode(object);
	}
}
