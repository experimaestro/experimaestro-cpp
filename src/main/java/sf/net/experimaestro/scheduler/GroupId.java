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

import com.sleepycat.persist.model.KeyField;
import com.sleepycat.persist.model.Persistent;

import static java.lang.StrictMath.min;

/**
 * Identifier for a group of resources
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 29/1/13
 */
@Persistent
public class GroupId implements Comparable<GroupId> {
    @KeyField(1)
    private String name = "";

    protected GroupId() {
    }

    public GroupId(String group) {
        this.name = group;
    }


    public static GroupId end(String group) {
        return new GroupId(group + ".");
    }


    @Override
    public String toString() {
        return name;
    }

    /**
     * Compares two strings with special rules linked to the dot (a and b are sequences of non dots):
     *
     * <ol>
     *     <li>a.b is less than ab</li>
     *     <li>a.b is greater than a</li>
     *     <li>a.b. is less than a.</li>
     * </ol>
     *
     *
     * @param o
     * @return
     */
    @Override
    public int compareTo(GroupId o) {
        // Compares give a special meaning to dots
        int l1 = this.name.length(), l2 = o.name.length();

        int l = min(l1,l2);

        // true if the last common character is a dot
        boolean lastDots = false;

        for(int i = 0; i < l; i++) {
            lastDots = false;
            char c1 = this.name.charAt(i);
            char c2 = o.name.charAt(i);

            if (c1 == '.') {
                if (c2 == '.') {
                    lastDots = true;
                    continue;
                }
                return -1;
            }

            if (c2 == '.') {
                return 1;
            }

            int z = Character.compare(c1, c2);
            if (z != 0)
                return z;

        }

        // Otherwise, based on length (the lengthier is smaller since we want a.b. to be after a.b.c)
        return lastDots ? Integer.compare(l2, l1) : Integer.compare(l1, l2);
    }

    public String getName() {
        return name;
    }
}
