package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;

/**
 * Task factory as seen by JavaScript
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TaskFactoryJSWrapper extends ScriptableObject {
	private static final long serialVersionUID = 1L;

	private TaskFactory information;

	public TaskFactoryJSWrapper() {
	}

	public void jsConstructor(Scriptable information) {
		if (information != null)
			this.information = (TaskFactory) ((NativeJavaObject) information)
					.unwrap();
	}

	@Override
	public String getClassName() {
		return "XPMTaskFactory";
	}

	// ---- JavaScript functions ----

	public Scriptable jsFunction_create() {
		Task task = information.create();
		return Context.getCurrentContext().newObject(getParentScope(), "XPMTask",
				new Object[] { task });
	}


	
}