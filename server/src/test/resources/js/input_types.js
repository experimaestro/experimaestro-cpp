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

// Check the input types
 
var test = new Namespace("xpm.tests");

tasks("inputs") = {
     inputs: {
         // Values
         x_integer: { value: "xp:integer", optional: true },
         
         // XML types
         x_xml: { xml: "a", optional: true },
         x_ns_xml: { xml: "test:a", optional: true },
         
         // Sequence
         x_sequence: { xml: "a", sequence: true, optional: true }
     }
};

// Assert that the plan generates an error
function assert_error(plan) {
    var ok = false;
    try {
        tasks("inputs").run(plan);
        ok = false;
    } catch(e) {
        ok = true;
    }
    if (!ok)
        throw "Wrong input type was not detected: " + plan.toSource();
}

function assert_ok(plan) {
    var ok = false;
    try {
        tasks("inputs").run(plan);
        ok = true;
    } catch(e) {
        logger.warn("Exception: %s", e);
        ok = false;
    }
    if (!ok)
        throw "Wrong input type was detected but it should not have: " + plan.toSource();
} 


// ---- TESTS ----
 
function test_integer() {
    assert_ok({ x_integer: 1 });
    assert_ok({ x_integer: { "$type": "xp:integer", "$value": 1 } });
    
    assert_error({ x_integer: 1.2 });        
    assert_error({ x_integer: <a>hello</a> });        
}
 
// function test_xml() {
//     assert_ok({ x_xml: <a>1</a> });    
//     
//     assert_error({ x_xml: <a xmlns={test.uri}>1</a> });    
//     assert_error({ x_xml: <b>1</b> });    
// }
// 
// function test_ns_xml() {
//     assert_ok({ x_ns_xml: <a xmlns={test.uri}>1</a> });    
//     
//     assert_error({ x_ns_xml: <a xmlns="other.ns">1</a> });    
//     assert_error({ x_ns_xml: <b>1</b> });    
// }
