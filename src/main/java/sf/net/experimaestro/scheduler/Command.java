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

package sf.net.experimaestro.scheduler;

import com.sleepycat.persist.model.Persistent;

import java.util.*;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 18/10/12
 */
@Persistent
public class Command implements Iterable<AbstractCommandArgument> {
    ArrayList<AbstractCommandArgument> list;

    public Command() {
        list = new ArrayList<>();
    }

    public Command(Collection<? extends CommandArgument> c) {
        list = new ArrayList<>(c);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for(AbstractCommandArgument argument: list) {
            sb.append('\'');
            sb.append(argument.toString());
            sb.append('\'');
            if (first)
                first = false;
            else
                sb.append(',');
        }
        sb.append("]");
        return sb.toString();
    }


    public int size() {
        return list.size();
    }

    @Override
    public Iterator<AbstractCommandArgument> iterator() {
        return list.iterator();
    }

    public void add(AbstractCommandArgument argument) {
        list.add(argument);
    }
}
