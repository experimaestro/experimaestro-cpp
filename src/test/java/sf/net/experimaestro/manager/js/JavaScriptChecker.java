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

package sf.net.experimaestro.manager.js;

import org.apache.commons.vfs2.FileObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;
import org.testng.annotations.*;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.connectors.XPMConnector;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.scheduler.ResourceLocator;
import sf.net.experimaestro.utils.Cleaner;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.XPMEnvironment;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
* @author B. Piwowarski <benjamin@bpiwowar.net>
* @date 30/1/13
*/
public class JavaScriptChecker {

    private final XPMEnvironment environment;
    private FileObject file;
    private String content;
    private Context context;
    private Repository repository;
    private ScriptableObject scope;
    private boolean initialized;

    public JavaScriptChecker(XPMEnvironment environment, FileObject file) throws
            IOException {
        this.environment = environment;
        environment.init();
        this.file = file;
        this.content = getFileContent(file);
    }

    @AfterClass
    public void close() {
        environment.close();
    }

    @Override
    public String toString() {
        return format("JavaScript for [%s]", file);
    }


    @DataProvider
    public Object[][] jsProvider() throws IOException {
        Pattern testFunctionPattern = Pattern.compile("function\\s+(test_[\\w]+)\\s*\\(");
        Matcher matcher = testFunctionPattern.matcher(content);
        ArrayList<Object[]> list = new ArrayList<>();

        // Adds the script
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
        repository = new Repository(new ResourceLocator(XPMConnector.getInstance(), "/"));
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
            initialized = false;
            // Defines the environment
            Map<String, String> environment = System.getenv();
            final ResourceLocator currentResourceLocator
                    = new ResourceLocator(LocalhostConnector.getInstance(), file.getName().getPath());
            XPMObject jsXPM = new XPMObject(currentResourceLocator, context, environment, scope,
                    repository, this.environment.scheduler, null, new Cleaner());

            // Adds some special converter available for tests only
            JSUtils.addFunction(SSHServer.class, scope, "sshd_server", new Class[]{});

            context.evaluateReader(scope, new StringReader(content),
                    file.toString(), 1, null);
            initialized = true;
        } else {
            assert initialized : "Not running test since initialization did not work";
            Object object = scope.get(functionName, scope);
            assert object instanceof Function : format(
                    "%s is not a function", functionName);
            Function function = (Function) object;
            context.setWrapFactory(JSBaseObject.XPMWrapFactory.INSTANCE);
            function.call(context, scope, null, new Object[]{});
        }
    }


    static String getFileContent(FileObject file)
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
