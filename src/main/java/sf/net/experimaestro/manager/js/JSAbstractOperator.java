package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import sf.net.experimaestro.manager.plans.FunctionOperator;
import sf.net.experimaestro.manager.plans.GroupBy;
import sf.net.experimaestro.manager.plans.Operator;
import sf.net.experimaestro.manager.plans.Order;
import sf.net.experimaestro.manager.plans.OrderBy;
import sf.net.experimaestro.manager.plans.XPathFunction;
import sf.net.experimaestro.utils.JSUtils;

import javax.xml.xpath.XPathExpressionException;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 8/3/13
 */
public abstract class JSAbstractOperator extends JSBaseObject {
    /** Get the associated operator */
    abstract Operator getOperator();

    @JSFunction(scope = true)
    @JSHelp("Runs an XQuery against the input: each returned item is a new input")
    public JSAbstractOperator xpath(Context context, Scriptable scope, String xpath) throws XPathExpressionException {
        XPathFunction function = new XPathFunction(xpath, JSUtils.getNamespaceContext(scope));
        Operator operator = new FunctionOperator(function);
        operator.addParent(this.getOperator());
        return new JSOperator(getOperator());
    }

    @JSFunction
    public JSOperator group_by(JSOperator... operators) {
        GroupBy groupBy = new GroupBy();

        // Order using the operators we should group by
        Order<Operator> order = new Order();
        for (JSOperator op : operators)
            order.add(op.getOperator(), false);
        OrderBy orderBy = new OrderBy(order, null);
        orderBy.addParent(getOperator());

        groupBy.addParent(orderBy);

        return new JSOperator(groupBy);
    }

    @JSFunction
    public JSOperator copy() {
        return new JSOperator(getOperator().copy());
    }

}
