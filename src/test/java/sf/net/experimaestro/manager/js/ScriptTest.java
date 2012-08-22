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
import org.apache.log4j.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;
import org.testng.annotations.*;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.scheduler.ResourceLocator;
import sf.net.experimaestro.utils.JSUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Runs the scripts contained in the directory "test/resources/js"
 * <p/>
 * Tests are defined by matching javascript functions matching "function test_XXXX()"
 */
public class ScriptTest {
    static final Logger LOGGER = Logger.getLogger(ScriptTest.class);

    private static final String JS_SCRIPT_PATH = "/js";

    static public class JavaScriptChecker {

        private FileObject file;
        private String content;
        private Context context;
        private Repository repository;
        private ScriptableObject scope;

        public JavaScriptChecker(FileObject file) throws
                IOException {
            this.file = file;
            this.content = getFileContent(file);
        }

        @Override
        public String toString() {
            return format("JavaScript for [%s]", file);
        }

        @DataProvider
        public Object[][] jsProvider() throws IOException {
            Pattern pattern = Pattern
                    .compile("function\\s+(test_[\\w]+)\\s*\\(");
            Matcher matcher = pattern.matcher(content);
            ArrayList<Object[]> list = new ArrayList<>();
            list.add(new Object[]{null});

            while (matcher.find()) {
                list.add(new Object[]{matcher.group(1)});
            }

            return list.toArray(new Object[list.size()][]);
        }

        @BeforeTest
        public void enter() {
            context = Context.enter();
            scope = context.initStandardObjects();
            repository = new Repository(new ResourceLocator());

        }

        @AfterTest
        public void exit() {
            Context.exit();
        }

        @Test(dataProvider = "jsProvider")
        public void testScript(String functionName) throws
                IOException, SecurityException, IllegalAccessException,
                InstantiationException, InvocationTargetException,
                NoSuchMethodException {
            if (functionName == null) {
                // Defines the environment
                Map<String, String> environment = System.getenv();
                XPMObject jsXPM = new XPMObject(new ResourceLocator(), context, environment, scope,
                        repository, null);


                context.evaluateReader(scope, new StringReader(content),
                        file.toString(), 1, null);
            } else {
                Object object = scope.get(functionName, scope);
                assert object instanceof Function : format(
                        "%s is not a function", functionName);
                Function function = (Function) object;
                function.call(context, scope, null, new Object[]{});
            }
        }
    }

    @Factory
    public static Object[] jsFactories() throws IOException {
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
                return file.getFile().getName().getExtension().equals("js");
            }
        });

        Object[] r = new Object[files.length];
        for (int i = r.length; --i >= 0; )
            r[i] = new JavaScriptChecker(files[i]);

        return r;
    }

    private static String getFileContent(FileObject file)
            throws IOException {
        InputStreamReader reader = new InputStreamReader(file.getContent()
                .getInputStream());
        char[] cbuf = new char[8192];
        int read = 0;
        StringBuilder builder = new StringBuilder();
        while ((read = reader.read(cbuf, 0, cbuf.length)) > 0)
            builder.append(cbuf, 0, read);
        String s = builder.toString();
        return s;
    }


}