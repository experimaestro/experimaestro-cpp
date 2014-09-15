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

package sf.net.experimaestro.utils;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/3/13
 */
public class RangeUtils {
    /** Transforms a range into an array of {@linkplain Integer} */
    public static Integer[] toIntegerArray(Range<Integer> closed) {
        ContiguousSet<Integer> integers = ContiguousSet.create(closed, DiscreteDomain.integers());
        return integers.toArray(new Integer[integers.size()]);
    }
}
