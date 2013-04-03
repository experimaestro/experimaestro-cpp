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
var abc = new Namespace("a.b.c");

// The task
tasks("abc:task") = {    
    // One input of type xs:integer
    inputs: {
        x: { value: "xp:integer" },
        y: { value: "xp:integer" }
    },
    
    // The function that will be called when the task is run_plan
	run: function(inputs) {
        // Returns something
		return inputs.x() * inputs.y();
	}
		
};

// END SNIPPET: main

/** Run and check */

function check(results, expected) {
    results.sort(function(x,y) { return x - y; });
    for(var i = 0; i < expected.length; i++) {
        if (expected[i] != results[i])
            throw new java.lang.String.format("Expected %s and got %s at %s", expected[i], results[i], i);
    }
}

function test_plan_1() {
    // START SNIPPET: check
    var results = tasks("abc:task").run({
            x: [1, 2],
            y: [5, 7]        
    });
    logger.info("The task returned %s\n", results.toSource());
    // END SNIPPET: check
    check(results, [5, 7, 10, 14]);
}

