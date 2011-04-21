package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.utils.log.Logger;

/**
 * Task factory as seen by JavaScript
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TaskFactoryJSWrapper extends ScriptableObject {
	final static private Logger LOGGER = Logger.getLogger();
	
	private static final long serialVersionUID = 1L;

	public static final String CLASSNAME = "XPMTaskFactory";

	TaskFactory factory;

	public TaskFactoryJSWrapper() {
	}

	public void jsConstructor(Scriptable information) {
		if (information != null) {
			LOGGER.info(information.toString());
			this.factory = (TaskFactory) ((NativeJavaObject) information)
					.unwrap();
		}
	}

	@Override
	public String getClassName() {
		return "XPMTaskFactory";
	}

	// ---- JavaScript functions ----

	public Scriptable jsFunction_create() {
		Task task = factory.create();
		return Context.getCurrentContext().newObject(getParentScope(), "XPMTask",
				new Object[] { task });
	}


	
}