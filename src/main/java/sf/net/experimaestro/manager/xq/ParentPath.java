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

package sf.net.experimaestro.manager.xq;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import sf.net.experimaestro.manager.Manager;

import java.io.File;

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
