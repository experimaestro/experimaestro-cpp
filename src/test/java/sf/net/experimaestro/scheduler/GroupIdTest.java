/*
 * This file is part of experimaestro.
 * Copyright (c) 2013 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.scheduler;

import org.testng.annotations.Test;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 29/1/13
 */
public class GroupIdTest {
    static int compare(String a, String b) {
        return new GroupId(a).compareTo(new GroupId(b));
    }

    @Test
    public void testEquals() {
        assert compare("a.b", "a.b") == 0;
    }

    @Test
    public void testDifferent() {
        assert compare("a.b", "ab") == -1;
        assert compare("ab", "a.b") == 1;
    }

    @Test
    public void testInside() {
        assert compare("a", "a.b") == -1;
        assert compare("a.b", "a") == 1;
    }

    @Test
    public void testLast() {
        assert compare("a.", "a.b") == 1;
        assert compare("a.b", "a.") == -1;
    }


}
