5/*
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

/*
    Goal:
    - plans should be easily composable
    - plans can be simplified (factorization) before being run
 */

include("check_array.inc.js");
 
 // START SNIPPET: task
var ns = new Namespace("xpm.tests");
var logger = xpm.logger("xpm.tests");

tasks.add("ns:mult", {
    inputs: {
        x: { value: "xp:integer" },
        y: { value: "xp:integer" }
    },

    run: function(x) {
        logger.debug("Task mult: got x=%s and y=%s: %s", $(x.x), $(x.y), $(x.x) * $(x.y));
        return $(x.x) * $(x.y);
    }
});

tasks.add("ns:plus", {
    inputs: {
        x: { value: "xp:integer" },
        y: { value: "xp:integer" }
    },

    run: function(x) {
        logger.debug("Task plus: got x=%s and y=%s: %s", $(x.x), $(x.y), $(x.x) + $(x.y));
        return $(x.x) + $(x.y)
    }
});
// END SNIPPET: task

tasks.add("ns:identity", {
    inputs: {
        x: {
            value: "xp:integer"
        }
    },

    run: function(x) {
        return x.x;
    }
});

/**
 * Simple product of two plans
 */
function test_simple() {
    // Plan
    var plan1 = tasks("ns:identity").plan({
        x: [1, 2]
    });
    var plan2 = tasks("ns:mult").plan({
        x: plan1,
        y: [3, 5]
    });

    // Optimize and run
    // Should be an array of values 3, 6, 5, 10
    var result = plan2();
    logger.info("Result is %s", result);
    check_array(result, [3, 5, 6, 10]);
}



// We apply some transformation on the output of a task

function test_transform() {
    var f = function(x) {
        return Number($(x)) + 1;
    };


    var plan1 = tasks("ns:identity").plan({
        x: [0, 1]
    });

    var plan2 = tasks("ns:mult").plan({
        x: transform(f, plan1),
        y: [3, 5]
    });

    // Optimize and run
    // Should be an array of XML values 3, 6, 5, 10
    var result = plan2.run();
    check_array(result, [3, 5, 6, 10]);
}

// Joining tasks
// Goal: computes x + (x * y)

function test_join() {
    var plan1 = tasks("ns:identity").plan({
        x: [1, 2]
    });
    var plan2 = tasks("ns:mult").plan({
        x: plan1,
        y: [3, 5]
    });
    var plan3 = tasks("ns:plus").plan(
        {
            x: plan1,
            y: plan2
        }
    );
    
    var result = plan3.run();
    check_array(result, [4, 6, 8, 12]);
}

// Test a join with an union
function test_join_union() {
    // Join plan1 of plan2 with plan1 (within plan2)
	// note that because x in plan2 has an extra input
	// this gives a cartesian product with results
	// plus([-3, -5], [1, 2]

    var plan1 = tasks("ns:identity").plan({
        x: [1, 2]
    });
    var plan2 = tasks("ns:mult").plan({
        x: [plan1, -1],
        y: [3, 5]
    });
    var plan3 = tasks("ns:plus").plan(
        {
            x: plan1,
            y: plan2
        }
    );
    
    var result = plan3.run();
    check_array(result, [-4, -3, -2, -1, 4, 6, 8, 12]);
}

// Implicit join 
function test_implicit_join() {
    var plan1 = tasks("ns:identity").plan({
        x: [2, 3]
    });
    var plan2 = tasks("ns:plus").plan({
        x: plan1,
        y: plan1
    });
    var result = plan2.run();
    check_array(result, [4, 6]);
}


// Copy of a plan to perform cartesian products
function test_product() {
    var plan1 = tasks("ns:identity").plan({
        x: [2, 3]
    });
    var plan2 = tasks("ns:plus").plan({
        x: plan1,
        y: plan1.copy(),
    });
    var result = plan2();
    check_array(result, [4, 5, 5, 6]);
}

function test_example() {
// START SNIPPET: run
    // Creates the experimental plan
    var plan1 = tasks("ns:plus").plan({
        x: [1, 2],
        y: 3
    });
    var plan2 = tasks("ns:mult").plan({
        // The values from x will come from the output of plan1
        x: plan1,
        y: 2
    });
    
    // Executes the experimental plan
    // Result is an array containing the values (in XML) 8 and 10
    var result = plan2();

// END SNIPPET: run    
}



// Test plans building by adding
function test_add() {
    var plan = tasks("ns:plus").plan({ x: [1, 2], y: 3 });
    plan.add({ x: [4, 5], y: 2});
    
    var result = plan();
    logger.info("Array is %s", result);
    check_array(result, [4, 5, 6, 7]);
}


// --- Test the group by

tasks.add("ns:sum", {
    inputs: {
        x: { value: "xp:integer", sequence: true }
    },

    run: function(p) {
        var sum = 0;
        for(var i = 0; i < p.x.length; i++){
            sum += $(p.x[i]);
        }            
        return sum;
    }
});

function test_groupby_all() {
    var plan1 = tasks("ns:identity").plan({ x: [1, 2, 3] });
    var plan2 = tasks("ns:sum").plan({ x: plan1.group_by() });
    
    var result = plan2();
    check_array(result, [6]);
}

function test_groupby() {
    var plan1 = tasks("ns:identity").plan({ x: [1, 2, 3] });
    var plan2 = tasks("ns:plus").plan({ x: plan1, y: [10, 20] });
    var plan3 = tasks("ns:sum").plan({ x: plan2.group_by(plan1) });
    
    var result = plan3();
    check_array(result, [32, 34, 36]);
}