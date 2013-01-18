package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.ScriptRunner;
import sf.net.experimaestro.utils.JSUtils;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 18/1/13
 */
public class JSScriptRunner implements ScriptRunner {
    private Scriptable scope;

    public JSScriptRunner(Scriptable scope) {
        this.scope = scope;
    }


    @Override
    public Object evaluate(String script) throws Exception {
        final Object result = Context.getCurrentContext().evaluateString(scope, script, "inline", 1, null);
        if (JSUtils.isXML(result))
            return JSUtils.toDocument(result, new QName(Manager.EXPERIMAESTRO_NS, "parameters"));
        return JSUtils.toString(result);
    }
}
