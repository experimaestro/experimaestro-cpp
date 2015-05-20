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

import com.google.common.collect.ImmutableList;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.spi.LoggerRepository;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptStackElement;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.testng.TestException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import sf.net.experimaestro.manager.Repositories;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.manager.scripting.MethodFunction;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.XPMEnvironment;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
public class JavaScriptChecker extends XPMEnvironment {
    final static private Logger LOGGER = Logger.getLogger();

    private final String SSHD_SERVER_FUNCTION = "sshd_server";

    private JavaScriptRunner jcx;

    private Path file;

    private String content;

    private Context context;

    private Repository repository;

    private Scriptable scope;

    public JavaScriptChecker(Path file) throws
            Throwable {
        this.file = file;
        this.content = getFileContent(file);

        // Initialize XPM and load the file
        context = Context.enter();
        repository = new Repository(new File("/").toPath());
        Map<String, String> environment = System.getenv();

        final Scheduler scheduler = prepare().getScheduler();
        Repositories repositories = new Repositories(null);
        repositories.add(repository, 0);
        final LoggerRepository loggerRepository = LOGGER.getLoggerRepository();

        jcx = new JavaScriptRunner(repositories, scheduler, (Hierarchy) loggerRepository, null);
        final MethodFunction method = new MethodFunction(SSHD_SERVER_FUNCTION);
        final Method sshd_server = SSHServer.class.getDeclaredMethod("sshd_server");
        method.add(null, ImmutableList.of(sshd_server));
        ScriptableObject.defineProperty(jcx.scope, SSHD_SERVER_FUNCTION, method, 0);

        // Evaluate
        jcx.evaluateString(content, file.toString(), 0, null);
        scope = jcx.scope;
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

    @Override
    public String toString() {
        return format("JavaScript for [%s]", file);
    }

    @AfterClass
    public void exit() throws Exception {
        jcx.close();
        Context.exit();
    }

    @DataProvider
    public Object[][] jsProvider() throws IOException {
        Object[] ids = jcx.scope.getIds();
        String prefix = "test_";
        ArrayList<Object[]> list = new ArrayList<>();

        for (Object id : ids) {
            String name = id.toString();
            if (name.startsWith(prefix)) {
                Object o = scope.get(name, scope);
                if (o instanceof Function)
                    list.add(new Object[]{new JSTestFunction(name.substring(prefix.length()), (Function) o)});
            }

        }
        return list.toArray(new Object[list.size()][]);
    }


    @Test(dataProvider = "jsProvider")
    public void testScript(JSTestFunction testFunction) throws
            IOException, SecurityException, IllegalAccessException,
            InstantiationException, InvocationTargetException,
            NoSuchMethodException {
        Scriptable newScope = context.newObject(scope);
        newScope.setPrototype(scope);
        newScope.setParentScope(null);
        try {
            testFunction.function.call(context, newScope, null, new Object[]{});
        } catch (Throwable e) {
            LOGGER.error(e, "Error while running %s", testFunction.name);

            Throwable wrapped = e;
            LOGGER.info("Exception thrown there: %s", e.getStackTrace()[0]);
            while (wrapped.getCause() != null)
                wrapped = wrapped.getCause();

            final ScriptStackElement[] scriptStackTrace = JSUtils.getScriptStackTrace(wrapped);
            for (ScriptStackElement x : scriptStackTrace) {
                LOGGER.error(format("  at %s:%d (%s)", x.fileName, x.lineNumber, x.functionName));
            }
            throw new TestException(format("JS error for %s", testFunction.name));
        }
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
            return "test [" + name + "]";
        }
    }

}
