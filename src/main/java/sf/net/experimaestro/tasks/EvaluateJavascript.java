package sf.net.experimaestro.tasks;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import sf.net.experimaestro.tasks.ServerTask.RPCTaskManager.JSGetEnv;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.log.Logger;

import bpiwowar.argparser.Argument;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;

@TaskDescription(name = "evaluate-javascript", project = { "xpmanager" }, description = "Evaluate locally some javascript (debug purposes)")
public class EvaluateJavascript extends AbstractTask {
	final static private Logger LOGGER = Logger.getLogger();

	@Argument(name = "script", help = "The script to execute (null for standard input)")
	File file;

	@Override
	public int execute() throws Throwable {
		InputStreamReader in;
		if (file == null)
			in = new InputStreamReader(System.in);
		else
			in = new FileReader(file);

		StringBuffer sb = new StringBuffer();
		int len = 0;
		char[] buffer = new char[8192];
		while ((len = in.read(buffer)) >= 0)
			sb.append(buffer, 0, len);

		org.mozilla.javascript.Context cx = org.mozilla.javascript.Context
				.enter();

		Scriptable scope = cx.initStandardObjects();

		Object result = cx.evaluateString(scope, sb.toString(), "stdin", 1,
				null);

		if (result != null) {
			LOGGER.info("Class of returned result: %s", result.getClass());
			LOGGER.info(result.toString());
		} else
			LOGGER.info("Null result");

		return 0;
	}
}
