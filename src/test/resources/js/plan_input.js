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

include("check_array.inc.js");

function test_simple() {
    var input = new PlanInput([
        <a>1</a>,
        <a>2</a>
        ]);

        tasks("test") = {
            inputs: { x: { xml: "a" } },
            run: function(p) {
                return p.x.text();
            }
        }

        var result = tasks("test").run({ x: input });
        check_array(result, [1, 2]);
}

function test_simple() {
    var input = new PlanInput([
        <a><b>10</b><c>1</c><c>2</c></a>,
        <a><b>20</b><c>3</c></a>
        ]);

        tasks("plus") = {
            inputs: { b: { xml: "b" }, c: { xml: "c" } },
            run: function(p) {
                return Number(p.b.text()) + Number(p.c.text());
            }
        }

        var plan = tasks("plus").plan({ b: input.xpath("/a/b"), c: input.xpath("/a/c") });
        logger.debug("Plan:%n%s%n", plan.to_dot(true));
        var result = plan.run();
        check_array(result, [11, 12, 23]);
}