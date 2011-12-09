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

package sf.net.experimaestro.manager;


import org.w3c.dom.Document;

import sf.net.experimaestro.exceptions.ExperimaestroException;

public class TaskInput extends Input {

	final TaskFactory factory;

	public TaskInput(TaskFactory factory, QName type) {
		super(type);
		this.factory = factory;
	}

	@Override
	public void setDefaultValue(Document defaultValue) {
		throw new ExperimaestroException(
				"Default value must not be set for task inputs");
	}

	@Override
	Value newValue() {
		return new TaskValue(this);
	}
}
