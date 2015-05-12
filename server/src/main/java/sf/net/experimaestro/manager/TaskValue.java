package sf.net.experimaestro.manager;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
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

import sf.net.experimaestro.exceptions.NoSuchParameter;
import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.plans.ScriptContext;

public class TaskValue extends Value {
    private Task task;
    private Json value;

    public TaskValue() {
    }


    public TaskValue(TaskInput taskInput) {
        super(taskInput);
        task = taskInput.factory.create();
    }


    @Override
    public Value getValue(DotName id) throws NoSuchParameter {
        if (id.size() == 0)
            return this;
        return task.getValue(id);
    }

    @Override
    public void set(Json value) {
        this.value = value;
    }

    @Override
    public void process(ScriptContext taskContext) throws NoSuchParameter, ValueMismatchException {
        if (value == null) {
            // Run unless a value was set
            value = task.run(taskContext);
        }
    }

    @Override
    public Json get() {
        return value;
    }

    @Override
    protected void init(Value _other) {
        TaskValue other = (TaskValue) _other;
        super.init(other);
        task = other.task.copy();
    }

}