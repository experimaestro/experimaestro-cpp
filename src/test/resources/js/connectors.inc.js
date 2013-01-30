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

var python_script = script_file().get_parent().path("connectors.py");

var task = {
    // The id of the task is an XML qualified name 
    id: qname("a.b.c", "task"),
    
    // One input of type xp:integer
    inputs: <inputs><value type="xs:integer" id="x"/></inputs>,
    
    // The function that will be called when the task is run
	run: function(inputs) {
        v = xpm.evaluate(["python", python_script]);
		return <outputs>{v[1]}</outputs>;
	}
		
};

// Add the task to the list of available factories
xpm.add_task_factory(task);
