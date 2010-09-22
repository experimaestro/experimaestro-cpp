package bpiwowar.expmanager.tasks;

import java.util.Arrays;
import java.util.List;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.ExtensionFunctionCall;
import net.sf.saxon.functions.ExtensionFunctionDefinition;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SingletonIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.tinytree.TinyElementImpl;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.SequenceType;

import org.apache.log4j.Level;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import bpiwowar.argparser.ArgParseException;
import bpiwowar.argparser.ArgParser;
import bpiwowar.argparser.ArgParserOption;
import bpiwowar.log.Logger;

public class Main {

	final static private Logger logger = Logger.getLogger();;

	public static void main(String[] args) {
		try {
			new Main().run(args);
		} catch (Throwable e) {
			logger.error("Stopping because of an exception");
			logger.printException(Level.ERROR, e);
			// Exit with an error
			System.exit(1);
		}

		System.exit(0);
	}

	/**
	 * Main method
	 * 
	 * @param args
	 *            The command line arguments
	 */
	private void run(String[] args) throws Throwable {
		// Read options
		ArgParser argParser = new ArgParser("[options] <task>");
		argParser.addOptions(this);
		args = argParser.matchAllArgs(args, 0, ArgParserOption.EXIT_ON_ERROR,
				ArgParserOption.STOP_FIRST_UNMATCHED);

		if (args.length == 0)
			throw new ArgParseException("Expected a task");

		String task = args[0];
		args = Arrays.copyOfRange(args, 1, args.length);

		// --- Server task ---

		if ("xquery".equals(task)) {
			Configuration config = new Configuration();
			config.setAllowExternalFunctions(true);
			config.registerExtensionFunction(new TestFunctionDefinition());
			StaticQueryContext staticContext = config.newStaticQueryContext();
			// staticContext.declareGlobalVariable(qName, type, value,
			// external);
			staticContext.declareNamespace("sax", "net.bpiwowar.sax");
			DynamicQueryContext dynamicContext = new DynamicQueryContext(config);
			XQueryExpression exp = staticContext.compileQuery(args[0]);
			List<?> evaluate = exp.evaluate(dynamicContext);

			for (Object i : evaluate)
				if (i instanceof TinyElementImpl)
					logger.info("XQ: %s",
							((TinyElementImpl) i).getDisplayName());
				else
					logger.info("XQ: %s", i);

			//
			// XQDataSource xqjd = new SaxonXQDataSource();
			// XQConnection xqjc = xqjd.getConnection();
			// XQStaticContext xqsc = xqjc.getStaticContext();
			// xqsc.declareNamespace("double", "java:bpiwowar.expmanager.Main");
			// XQExpression xqje = xqjc.createExpression();
			// XQSequence xqjs = xqje.executeQuery(args[0]);
			//
			// xqjs.writeSequence(System.out, new Properties());
			// xqjc.close();
		}
		// Test
		else if ("test".equals(task)) {
			// Creates and enters a Context. The Context stores information
			// about the execution environment of a script.
			Context cx = Context.enter();
			try {
				// Initialize the standard objects (Object, Function, etc.)
				// This must be done before scripts can be executed. Returns
				// a scope object that we use in later calls.
				Scriptable scope = cx.initStandardObjects();

				// Collect the arguments into a single string.
				for (int i = 0; i < args.length; i++) {
					logger.info("Executing %s", args[i]);
					Object result = cx.evaluateString(scope, args[i], "<cmd>",
							1, null);
					System.err.println(result.getClass());
					// Convert the result to a string and print it.
					System.err.println(Context.toString(result));
				}

			} finally {
				// Exit from the context.
				Context.exit();
			}

		}

		// Unknown command
		else
			throw new ArgParseException("Task " + task + " does not exist");
	}

	/**
	 * Test for XQuery extensions
	 * 
	 * @author B. Piwowarski <benjamin@bpiwowar.net>
	 */
	public static class TestFunctionDefinition extends
			ExtensionFunctionDefinition {
		private static final long serialVersionUID = 1L;
		private static final StructuredQName qName = new StructuredQName("sax",
				"net.bpiwowar.sax", "test-function");

		@Override
		public ExtensionFunctionCall makeCallExpression() {
			return new ExtensionFunctionCall() {
				private static final long serialVersionUID = 1L;

				@Override
				public SequenceIterator call(SequenceIterator[] arguments,
						XPathContext context) throws XPathException {
					logger.info("Calling!");
					return SingletonIterator.makeIterator(BooleanValue.TRUE);
				}
			};
		}

		@Override
		public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
			return SequenceType.SINGLE_BOOLEAN;
		}

		@Override
		public int getMinimumNumberOfArguments() {
			return 0;
		}

		@Override
		public int getMaximumNumberOfArguments() {
			return 0;
		}

		@Override
		public StructuredQName getFunctionQName() {
			return qName;
		}

		@Override
		public SequenceType[] getArgumentTypes() {
			return new SequenceType[] { SequenceType.SINGLE_BOOLEAN };
		}
	}

}
