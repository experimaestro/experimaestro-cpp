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

package sf.net.experimaestro.manager.js;

import org.apache.commons.vfs2.*;
import org.testng.annotations.Factory;
import sf.net.experimaestro.utils.XPMEnvironment;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.net.URL;

/**
 * Runs the scripts contained in the directory "test/resources/js"
 * <p/>
 * Tests are defined by matching javascript functions matching "function test_XXXX()"
 */
public class ScriptTest  {
    static final Logger LOGGER = Logger.getLogger();

    private static final String JS_SCRIPT_PATH = "/js";


    /**
     * Retrieves all the .js files (excluding .inc.js)
     * @return
     * @throws IOException
     */
    @Factory
    public Object[] jsFactories() throws IOException {
        XPMEnvironment environment = new XPMEnvironment();

        // Get the JavaScript files
        final URL url = ScriptTest.class.getResource(JS_SCRIPT_PATH);
        FileSystemManager fsManager = VFS.getManager();
        FileObject dir = fsManager.resolveFile(url.toExternalForm());
        FileObject[] files = dir.findFiles(new FileSelector() {
            @Override
            public boolean traverseDescendents(FileSelectInfo info)
                    throws Exception {
                return true;
            }

            @Override
            public boolean includeFile(FileSelectInfo file) throws Exception {
                final String name = file.getFile().getName().toString();
                return name.endsWith(".js") && !name.endsWith(".inc.js");
            }
        });

        Object[] r = new Object[files.length];
        for (int i = r.length; --i >= 0; )
            r[i] = new JavaScriptChecker(environment, files[i]);

        return r;

    }


}