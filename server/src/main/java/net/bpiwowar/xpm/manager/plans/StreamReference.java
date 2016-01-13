package sf.net.experimaestro.manager.plans;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
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

/**
 * Gives the position of a stream within the inputs
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
final public class StreamReference {
    /**
     * The index of the input
     */
    final int inputIndex;

    /**
     * The index within the input stream
     */
    final int contextIndex;

    public StreamReference(int inputIndex, int contextIndex) {
        this.contextIndex = contextIndex;
        this.inputIndex = inputIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StreamReference that = (StreamReference) o;

        if (contextIndex != that.contextIndex) return false;
        return inputIndex == that.inputIndex;

    }

    @Override
    public int hashCode() {
        int result = inputIndex;
        result = 31 * result + contextIndex;
        return result;
    }
}
