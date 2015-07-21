/*
 * This file is part of experimaestro.
 * Copyright (c) 2015 B. Piwowarski <benjamin@bpiwowar.net>
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

tasks.add("ns:mult", {
    inputs: {
        x: { value: "xp:integer" },
        y: { value: "xp:integer" }
    },

    run: function(x) {
        return $(x.x) * $(x.y);
    }
});

//tasks.plan({ x: [1, 2], y: [3]}).parameters({oar: {cores: 1}});