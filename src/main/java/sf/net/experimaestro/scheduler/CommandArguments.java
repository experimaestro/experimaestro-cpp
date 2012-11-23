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
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.connectors.SingleHostConnector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 18/10/12
 */
@Persistent
public class CommandArguments {
    ArrayList<CommandArgument> list;

    public CommandArguments(int initialCapacity) {
        list = new ArrayList<>(initialCapacity);
    }

    public CommandArguments() {
        list = new ArrayList<>();
    }

    public CommandArguments(Collection<? extends CommandArgument> c) {
        list = new ArrayList<>(c);
    }

    public void add(CommandArgument c) {
        list.add(c);
    }

    public List<String> toStrings(final SingleHostConnector connector) throws FileSystemException {
        ArrayList<String> strings = new ArrayList<>();
        for(CommandArgument argument: list)
            strings.add(argument.resolve(connector));
        return strings;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for(CommandArgument argument: list) {
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
}
