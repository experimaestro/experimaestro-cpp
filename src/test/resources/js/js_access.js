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

// Test the direct JS access


function test_direct() 
{
    
    tasks("direct-access") = {
        inputs: { 
            x: { value: "xs:integer" },
        },
        run: function(x, v) {
            return v.x;
        }
    }

    var r = tasks("direct-access").run({
        x: 1
    });
    
    assert(r[0].get_value() == 1, "%s is not equal to 1", r[0].get_string());
}

// function test_direct_xml() 
{
    tasks("direct-access-xml") = {
        inputs: { 
            a: { xml: "a" },
        },
        run: function(x, v) {
            return v.a.b;
        }
    }

    var r = tasks("direct-access-xml").run({
        a: <a><b xmlns:xp={xp.uri} xp:name="b" xp:value="1"></b></a>
    });
    
    assert(r[0].get_value() == 1, "%s is not equal to 1", r[0].get_string());
    
}
