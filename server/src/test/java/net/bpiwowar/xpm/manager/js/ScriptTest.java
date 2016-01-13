package sf.net.experimaestro.manager.js;

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

import org.testng.annotations.Factory;
import sf.net.experimaestro.utils.Functional;
import sf.net.experimaestro.utils.XPMEnvironment;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Runs the scripts contained in the directory "test/resources/js"
 * <p/>
 * Tests are defined by matching javascript functions matching "function test_XXXX()"
 */
public class ScriptTest  {
    static final Logger LOGGER = Logger.getLogger();

    private static final String JS_SCRIPT_PATH = "/js";

    /** If set, we will only use the specified file for testing */
    public static final String JS_TEST_FILE_KEY = "sf.net.experimaestro.manager.js.test.file";


    /**
     * Retrieves all the .js files (excluding .inc.js)
     * @return
     * @throws IOException
     */
    @Factory
    public Object[] jsFactories() throws Throwable {
        final XPMEnvironment environment = new XPMEnvironment();

        final String testFile = System.getProperty(JS_TEST_FILE_KEY);

        // Get the JavaScript files
        final URL url = ScriptTest.class.getResource(JS_SCRIPT_PATH);
        Path dir = Paths.get(url.toURI());

        return Files.walk(dir)
                .filter(path ->
                        path.getFileName().toString().endsWith(".js")
                        && !path.getFileName().toString().endsWith(".inc.js")
                        && (testFile == null || path.getFileName().toString().equals(testFile)))
                .map(Functional.propagateFunction(path -> new JavaScriptChecker(path)))
                .toArray(n -> new JavaScriptChecker[n]);
    }


}