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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.testng.annotations.*;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.utils.Cleaner;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.XPMEnvironment;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

import static java.lang.String.format;

/**
 * Unit tests using javascript files
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JavaScriptChecker {

    private final XPMEnvironment environment;
    private Path file;
    private String content;
    private Context context;
    private Repository repository;

    private Scriptable scope;

    public JavaScriptChecker(XPMEnvironment environment, Path file) throws
            IOException {
        this.environment = environment;
        this.file = file;
        this.content = getFileContent(file);
    }

    @Override
    public String toString() {
        return format("JavaScript for [%s]", file);
    }


    @BeforeClass
    public void runScript() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        // Defines the environment
        Map<String, String> environment = System.getenv();
        scope = XPMContext.newScope();
        XPMObject xpm = new XPMObject(LocalhostConnector.getInstance(), file, context, environment, scope,
                repository, this.environment.scheduler, null, new Cleaner(), null, null);

        // Adds some special functions available for tests only
        JSUtils.addFunction(SSHServer.class, scope, "sshd_server", new Class[]{});

        XPMObject.threadXPM.set(xpm);
        context.evaluateReader(scope, new StringReader(content),
                file.toString(), 1, null);

    }

    static public class JSTestFunction {
        private final String name;
        private final Function function;

        public JSTestFunction(String name, Function function) {
            this.name = name;
            this.function = function;
        }

        @Override
        public String toString() {
            return "test ["+name+"]";
        }
    }

    @DataProvider
    public Object[][] jsProvider() throws IOException {
        Object[] ids = scope.getIds();
        String prefix = "test_";
        ArrayList<Object[]> list = new ArrayList<>();

        for (Object id : ids) {
            String name = id.toString();
            if (name.startsWith(prefix)) {
                Object o = scope.get(name, scope);
                if (o instanceof Function)
                    list.add(new Object[]{ new JSTestFunction(name.substring(prefix.length()), (Function) o) });
            }

        }
        return list.toArray(new Object[list.size()][]);
    }

    @BeforeTest
    public void enter() {
        context = Context.enter();
        scope = context.initStandardObjects();
        repository = new Repository(new File("/").toPath());
    }

    @AfterTest
    public void exit() {
        Context.exit();
    }

    @Test(dataProvider = "jsProvider")
    public void testScript(JSTestFunction testFunction) throws
            IOException, SecurityException, IllegalAccessException,
            InstantiationException, InvocationTargetException,
            NoSuchMethodException {
        testFunction.function.call(context, scope, null, new Object[]{});
    }


    static String getFileContent(Path file)
            throws IOException {
        InputStreamReader reader = new InputStreamReader(Files.newInputStream(file));
        char[] cbuf = new char[8192];
        int read;
        StringBuilder builder = new StringBuilder();
        while ((read = reader.read(cbuf, 0, cbuf.length)) > 0)
            builder.append(cbuf, 0, read);
        return builder.toString();
    }

}
