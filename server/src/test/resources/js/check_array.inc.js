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

/** Check that the two arrays contain the same values, regardless of the order */

var logger = xpm.logger("xpm.tests");

function check_array(results, expected) {
	if (results.length != expected.length)
		throw new java.lang.String.format("The arrays differ in length (got %.0f, expected %.0f)", results.length, expected.length);
    
    // Sort the results
    logger.info("Results: %s", results.toSource());
    results.sort(function(x,y) { return $(x) - $(y); });
    for (var i = 0; i < expected.length; i++) {
        if (expected[i] != Number($(results[i]))) {
            logger.error("Expected %s and got %s at %s", expected[i].toSource(), $(results[i]), i);
			throw 1;
	    }
    }
}