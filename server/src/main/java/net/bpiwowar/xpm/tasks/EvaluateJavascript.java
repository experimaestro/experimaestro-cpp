package net.bpiwowar.xpm.tasks;

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

import bpiwowar.argparser.Argument;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import org.mozilla.javascript.Scriptable;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

@TaskDescription(name = "evaluate-javascript", project = {"xpmanager"}, description = "Evaluate locally some javascript (debug purposes)")
public class EvaluateJavascript extends AbstractTask {
    final static private Logger LOGGER = Logger.getLogger();

    @Argument(name = "script", help = "The script to execute (null for standard input)")
    File file;

    @Override
    public int execute() throws Throwable {
        InputStreamReader in;
        if (file == null)
            in = new InputStreamReader(System.in);
        else
            in = new FileReader(file);

        StringBuffer sb = new StringBuffer();
        int len = 0;
        char[] buffer = new char[8192];
        while ((len = in.read(buffer)) >= 0)
            sb.append(buffer, 0, len);

        org.mozilla.javascript.Context cx = org.mozilla.javascript.Context
                .enter();

        Scriptable scope = cx.initStandardObjects();

        Object result = cx.evaluateString(scope, sb.toString(), "stdin", 1,
                null);

        if (result != null) {
            LOGGER.info("Class of returned result: %s", result.getClass());
            LOGGER.info(result.toString());
        } else
            LOGGER.info("Null result");

        return 0;
    }
}
