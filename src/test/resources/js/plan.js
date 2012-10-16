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

/** Test experimental plans */


// START SNIPPET: main

// Used to store results
var results = [];

// The task
var task = {
    // The id of the task is an XML qualified name 
    id: qname("a.b.c", "task"),
    
    // One input of type xs:integer
    inputs: <inputs><value type="xs:integer" id="x"/><value type="xs:integer" id="y"/></inputs>,
    
    // The function that will be called when the task is run
	run: function(inputs) {
	    // Multiply x by y and add it to the results array
        results.push(inputs.x.@xp::value * inputs.y.@xp::value);
        // Returns something
		return <outputs>{inputs.x}{inputs.y}</outputs>;
	}
		
};

// Add the task to the list of available factories
xpm.add_task_factory(task);

// END SNIPPET: main

/** Run and check */

function check(expected) {
    for(var i = 0; i < expected.length; i++) {
        if (expected[i] != results[i])
            throw new java.lang.String.format("Expected %s and got %s at %s", expected[i], results[i], i);
}

function test_plan_1() {
    // START SNIPPET: check
    results = [];
    xpm.get_task(qname("a.b.c", "task")).run_plan("x=1,2 * y=5,7");
    xpm.log("The task returned\n%s", results.toSource());
    // END SNIPPET: check
    check([5, 10, 7, 14]);
}

