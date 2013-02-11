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

/*
    Goal:
    - plans should be easily composable
    - plans can be simplified (factorization) before being run
 */

ns = new Namespace("xpm.tests");

tasks.ns::task_1 = {
    inputs: {
        x: {
            value: "xs:integer"
        }
    },

    run: function(p) {
        return p.x;
    }
};

tasks.ns::task_1_bis = {
    inputs: {
        x: {
            value: "xs:integer"
        }
    },

    run: function(p) {
        return <a>{p.x}</a>;
    }
};

tasks.ns::task_2 = {
    inputs: {
        x: {
            value: "xs:integer"
        },
        y: {
            value: "xs:integer"
        }
    },

    run: function(p) {
        return p.x * p.y;
    }
};

tasks.ns::task_3 = {
    inputs: {
        x: {
            value: "xs:integer"
        },
        y: {
            value: "xs:integer"
        }
    },

    run: function(p) {
        return p.x + p.y;
    }
};

function check(results, expected) {
    for (var i = 0; i < expected.length; i++) {
        if (expected[i] != results[i]) throw new java.lang.String.format("Expected %s and got %s at %s", expected[i], results[i], i);
    }
}

/**
 * Simple product of two plans
 */
function test_simple() {
    // Plan
    var plan1 = tasks.ns::task_1.plan({
        x: [1, 2]
    });
    var plan2 = tasks.ns::task_2.plan({
        x: plan1,
        y: [3, 5]
    });

    // Optimize and run
    // Should be an array of XML values 3, 6, 5, 10
    var result = plan2();
    check(result, [3, 6, 5, 10]);
}


/**
 * Simple product of two plans (with access to output)
 */
function test_simple_access() {
    // Plan
    var plan1 = tasks.ns::task_1_bis.plan({
        x: [1, 2]
    });
    var plan2 = tasks.ns::task_2.plan({
        x: plan1.a,
        y: [3, 5]
    });

    // Optimize and run
    // Should be an array of XML values 3, 6, 5, 10
    logger.info(result);
    var result = plan2();
    check(result, [3, 6, 5, 10]);
}



// We apply some transformation on the output of a task

function test_transform() {
    var f = function() {
        return x + 1;
    };

    var plan1 = tasks.ns::task_1.plan({
        x: [0, 1]
    });
    var plan2 = tasks.ns::task_2.plan({
        x: transform(f, plan1().x),
        y: [3, 5]
    });

    // Optimize and run
    // Should be an array of XML values 3, 6, 5, 10
    logger.info(result);
    var result = plan2();
    check(result, [3, 6, 5, 10]);
}

// Joining tasks
// Goal: computes x + (x * y)

function test_join() {
    var plan1 = tasks.ns::task_1.plan({
        x: [1, 2]
    });
    var plan2 = tasks.ns::task_2.plan({
        x: plan1().x,
        y: [3, 5]
    });
    var plan3 = tasks.ns::task_3.plan(
        {
            x: plan1,
            y: plan2
        }
    );
    
    // Join plan1 of plan2 with plan1 (within plan2)
    plan3.join(plan1, plan2.plan1);
    
    var result = plan3();
    check(result, [4, 8, 6, 12]);
}


// Copy of a plan to perform cartesian products
function test_product() {
    var plan1_1 = tasks.ns::task_1.plan({
        x: [1, 2]
    });
    var plan1_2 = tasks.ns::task_1.plan({
        x: [3, 5]
    });
    var plan2 = tasks.ns::task_32.plan({
        x: plan1,
        y: plan1.copy(),
    });
    var result = plan3();
    check(result, [3, 6, 5, 10]);
}
