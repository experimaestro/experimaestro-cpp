package sf.net.experimaestro.manager.xq;

import java.io.File;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.ExtensionFunctionCall;
import net.sf.saxon.functions.ExtensionFunctionDefinition;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SingletonIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import sf.net.experimaestro.manager.Manager;

public class ParentPath extends ExtensionFunctionDefinition {
	private static final StructuredQName NAME = new StructuredQName(Manager.EXPERIMAESTRO_PREFIX, Manager.EXPERIMAESTRO_NS, "parentPath");
	private static final SequenceType[] SINGLE_STRING = new SequenceType[] { SequenceType.SINGLE_STRING };
	private static final long serialVersionUID = 1L;

	@Override
	public SequenceType[] getArgumentTypes() {
		return SINGLE_STRING;
	}

	@Override
	public StructuredQName getFunctionQName() {
		return NAME;
	}

	@Override
	public int getMinimumNumberOfArguments() {
		return 1;
	}
	
	@Override
	public int getMaximumNumberOfArguments() {
		return 1;
	}

	@Override
	public SequenceType getResultType(SequenceType[] arg0) {
		return SequenceType.SINGLE_STRING;
	}

	@Override
	public ExtensionFunctionCall makeCallExpression() {
		return new ExtensionFunctionCall() {
			private static final long serialVersionUID = 1L;

			@Override
			public SequenceIterator call(SequenceIterator[] arguments, XPathContext xpc)
					throws XPathException {
				String path = arguments[0].next().getStringValue();
				final String parentPath = new File(path).getParentFile().toString();
				return SingletonIterator.makeIterator(new net.sf.saxon.value.StringValue(parentPath));
			}
		};
	}

}
