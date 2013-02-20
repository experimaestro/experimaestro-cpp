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

package sf.net.experimaestro.manager;

import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.NoSuchParameter;
import sf.net.experimaestro.exceptions.ValueMismatchException;

public class TaskValue extends Value {
	private Task task;
	private Node value;

	public TaskValue() {
	}


    public TaskValue(TaskInput taskInput) {
		super(taskInput);
		task = taskInput.factory.create();
	}


    @Override
    public Value getValue(DotName id) throws NoSuchParameter {
        return task.getValue(id);
    }

    @Override
    public void set(Node value) {
        throw new IllegalArgumentException("Cannot set task value");
    }

    @Override
	public void process() throws NoSuchParameter, ValueMismatchException {
		value = task.run();
	}

	@Override
	public Node get() {
		return value;
	}

	@Override
	protected void init(Value _other) {
		TaskValue other = (TaskValue) _other;
		super.init(other);
		task = other.task.copy();
	}

}