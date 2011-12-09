/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2011 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

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

	public TaskFactory getFactory() {
		return factory;
	}


	
}