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

package sf.net.experimaestro.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.ArgumentClass;
import bpiwowar.argparser.OrderedArgument;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import org.apache.xmlrpc.client.XmlRpcClient;
import sf.net.experimaestro.tasks.config.XMLRPCClientConfig;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@TaskDescription(name = "test", project = { "xpmanager" })
public class Test extends AbstractTask {
	@Override
	public int execute() throws Throwable {
        // (?:\{(\w[\w\.:]+)\}|(\w+):)?((?:\w[-\.])+)
        // (?:\{(\w+)\}|(\w+):)?(?:\w+)

        Pattern QNAME_PATTERN = Pattern
                .compile("(?:\\{(\\w(?:\\w|[\\.:])+)\\}|(\\w+):)?((?:\\w|[-\\.])+)");


        String[] paths = new String[] {
                "a",
                "xp:a",
                "{uri.b}b"
        };
        for(String path: paths) {
            final Matcher matcher = QNAME_PATTERN.matcher(path);
            final boolean b = matcher.matches();
            System.err.format("Match “%s”: %b%n", path, b);

            if (b) {
                for(int i = 1; i <= matcher.groupCount(); i++)
                System.err.format(" [%d] %s%n", i,  matcher.group(i));
            }
        }

        return 0;
		
	}

}
