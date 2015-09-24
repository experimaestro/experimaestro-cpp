package sf.net.experimaestro.manager.python;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Undefined;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import sf.net.experimaestro.manager.Constants;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.json.*;
import sf.net.experimaestro.manager.scripting.ScriptContext;
import sf.net.experimaestro.manager.scripting.ScriptingPath;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.utils.JSUtils;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class PythonUtils {
    public static Json toJSON(Object value) {
        if (value instanceof sf.net.experimaestro.manager.scripting.Wrapper) {
            value = ((sf.net.experimaestro.manager.scripting.Wrapper) value).unwrap();
        }

        if (value instanceof Json)
            return (Json) value;

        // --- Simple cases
        if (value == null)
            return JsonNull.getSingleton();

        if (value instanceof Json)
            return (Json) value;

        if (value instanceof String)
            return new JsonString((String) value);

        if (value instanceof Double) {
            if ((double) ((Double) value).longValue() == (double) value)
                return new JsonInteger(((Double) value).longValue());
            return new JsonReal((Double) value);
        }
        if (value instanceof Float) {
            if ((double) ((Float) value).longValue() == (float) value)
                return new JsonInteger(((Float) value).longValue());
            return new JsonReal((Float) value);
        }

        if (value instanceof Integer)
            return new JsonInteger((Integer) value);

        if (value instanceof Long)
            return new JsonInteger((Long) value);

        if (value instanceof Boolean)
            return new JsonBoolean((Boolean) value);

        // --- A map
        if (value instanceof Map) {
            JsonObject json = new JsonObject();
            PythonNamespaceContext nsContext = new PythonNamespaceContext();
            for (Object o : ((Map) value).entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                QName qname = QName.parse(toString(entry.getKey()), nsContext);
                Object pValue = entry.getValue();

                if (qname.equals(Constants.XP_TYPE))
                    pValue = QName.parse(JSUtils.toString(pValue), nsContext).toString();

                String key = qname.toString();
                final Json key_value = toJSON(pValue);
                json.put(key, key_value);
            }
            return json;

        }

        // -- An array
        if (value instanceof List) {
            List array = (List) value;
            JsonArray json = new JsonArray();
            for (int i = 0; i < array.size(); ++i)
                json.add(toJSON(array.get(i)));
            return json;
        }

        if (value.getClass().isArray()) {
            final int length = Array.getLength(value);
            JsonArray json = new JsonArray();
            for (int i = 0; i < length; i++)
                json.add(toJSON(Array.get(value, i)));
            return json;
        }

        if (value instanceof java.nio.file.Path) {
            return new JsonPath((java.nio.file.Path) value);
        }

        if (value instanceof ScriptingPath)
            return new JsonPath(((ScriptingPath) value).getObject());

        if (value instanceof Resource)
            return new JsonResource((Resource) value);

        // -- Undefined
        if (value instanceof Undefined)
            return JsonNull.getSingleton();

        ScriptContext.get().getLogger("PythonUtils").warn("Transformed %s into a JSON string", value.getClass());

        return new JsonString(value.toString());
    }

    private static String toString(Object key) {
        return key.toString();
    }
}
