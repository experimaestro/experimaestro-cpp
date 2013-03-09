package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import sf.net.experimaestro.manager.plans.Constant;
import sf.net.experimaestro.manager.plans.FunctionOperator;
import sf.net.experimaestro.manager.plans.Operator;
import sf.net.experimaestro.manager.plans.XPathFunction;
import sf.net.experimaestro.utils.JSUtils;

import javax.xml.xpath.XPathExpressionException;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 8/3/13
 */
public class JSPlanInput extends JSBaseObject {
    Operator operator;

    public JSPlanInput(NativeArray array) {
        Constant constant = new Constant();
        this.operator = constant;
        for (Object o : array) {
            constant.add(JSUtils.toDocument(null, o));
        }
    }

    public JSPlanInput(Operator operator) {
        this.operator = operator;
    }

    @JSFunction(scope = true)
    @JSHelp("Runs an XQuery against the input: each returned item is a new input")
    public JSPlanInput xpath(Context context, Scriptable scope, String xpath) throws XPathExpressionException {
        XPathFunction function = new XPathFunction(xpath, JSUtils.getNamespaceContext(scope));
        Operator operator = new FunctionOperator(function);
        operator.addParent(this.operator);
        return new JSPlanInput(operator);
    }

}
