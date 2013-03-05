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

package sf.net.experimaestro.manager.plans;

/**
* @author B. Piwowarski <benjamin@bpiwowar.net>
* @date 26/2/13
*/
public class StreamReference {
    int streamIndex;
    int contextIndex;

    public StreamReference(int streamIndex, int contextIndex) {
        this.contextIndex = contextIndex;
        this.streamIndex = streamIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StreamReference that = (StreamReference) o;

        if (contextIndex != that.contextIndex) return false;
        if (streamIndex != that.streamIndex) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = streamIndex;
        result = 31 * result + contextIndex;
        return result;
    }
}
