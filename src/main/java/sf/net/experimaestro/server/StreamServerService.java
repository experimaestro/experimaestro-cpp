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

package sf.net.experimaestro.server;

import com.google.common.collect.Multiset;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.RootLogger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.exceptions.ContextualException;
import sf.net.experimaestro.manager.Repositories;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.manager.js.JSArgument;
import sf.net.experimaestro.manager.js.XPMObject;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.ResourceLocator;
import sf.net.experimaestro.scheduler.ResourceState;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.Cleaner;
import sf.net.experimaestro.utils.CloseableIterator;
import sf.net.experimaestro.utils.log.Logger;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The operation supported by the stream server
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 15/3/13
 */
// TODO: migrate (those with high streaming first) rpc calls from {@linkplain RPCHandler}
public class StreamServerService {
    final static private Logger LOGGER = Logger.getLogger();

    final private Repository repository;
    final private Scheduler scheduler;

    public StreamServerService(Repository repository, Scheduler scheduler) {
        this.repository = repository;
        this.scheduler = scheduler;
    }

    static class FilePointer {
        String filename;
        String content;

        FilePointer(String filename, String content) {
            this.filename = filename;
            this.content = content;
        }

        public boolean isFile() {
            return content == null;
        }
    }

    private EnumSet<ResourceState> getStates(Object[] states) {
        final EnumSet<ResourceState> statesSet;

        if (states == null || states.length == 0)
            statesSet = EnumSet.allOf(ResourceState.class);
        else {
            ResourceState statesArray[] = new ResourceState[states.length];
            for (int i = 0; i < states.length; i++)
                statesArray[i] = ResourceState.valueOf(states[i].toString());
            statesSet = EnumSet.of(statesArray[0], statesArray);
        }
        return statesSet;
    }


    /**
     * List jobs
     */
    @RPCHelp("List the jobs along with their states")
    public List<Map<String, String>> listJobs(String group, Object[] states) {
        final EnumSet<ResourceState> set = getStates(states);
        List<Map<String, String>> list = new ArrayList<>();

        for (Multiset.Entry<String> x : scheduler.subgroups(group).entrySet()) {
            Map<String, String> map = new HashMap<>();
            String s = x.getElement();
            map.put("type", "group");
            map.put("name", s);
//            map.put("count", Integer.toString(x.getCount()));
            list.add(map);
        }

        try (final CloseableIterator<Resource> resources = scheduler.resources(group, false, set, true)) {
            while (resources.hasNext()) {
                Resource resource = resources.next();
                Map<String, String> map = new HashMap<>();
                map.put("type", resource.getClass().getCanonicalName());
                map.put("state", resource.getState().toString());
                map.put("name", resource.getLocator().toString());
                list.add(map);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    /**
     * Run javascript
     *
     * @param packer
     * @param unpacker
     * @param files
     * @param environment
     */
    @RPCHelp("Run a javascript")
    public void runJSScript(final Packer packer, Unpacker unpacker,
                            @JSArgument(name = "files") List<FilePointer> files,
                            @JSArgument(name = "environment") Map<String, String> environment) {
        final StringWriter errString = new StringWriter();
        final PrintWriter err = new PrintWriter(errString);
        XPMObject jsXPM = null;

        final RootLogger root = new RootLogger(Level.INFO);
        final Hierarchy loggerRepository = new Hierarchy(root);
        BufferedWriter stringWriter = new BufferedWriter(new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                packer.write(new String(cbuf, off, len));
            }

            @Override
            public void flush() throws IOException {
                packer.flush();
            }

            @Override
            public void close() throws IOException {
                throw new UnsupportedOperationException();
            }
        });

        PatternLayout layout = new PatternLayout("%-6p [%c] %m%n");
        WriterAppender appender = new WriterAppender(layout, stringWriter);
        root.addAppender(appender);

        // Creates and enters a Context. The Context stores information
        // about the execution environment of a script.
        try (Cleaner cleaner = new Cleaner()) {
            Context jsContext = Context
                    .enter();

            // Initialize the standard objects (Object, Function, etc.)
            // This must be done before scripts can be executed. Returns
            // a scope object that we use in later calls.
            Scriptable scope = jsContext.initStandardObjects();


            // TODO: should be a one shot repository - ugly
            Repositories repositories = new Repositories(new ResourceLocator(LocalhostConnector.getInstance(), ""));
            repositories.add(repository, 0);

            ScriptableObject.defineProperty(scope, "env", new RPCHandler.JSGetEnv(environment), 0);
            jsXPM = new XPMObject(null, jsContext, environment, scope, repositories,
                    scheduler, loggerRepository, cleaner);

            Object result = null;
            for (int i = 0; i < files.size(); i++) {

                FilePointer filePointer = files.get(i);
                boolean isFile = filePointer.isFile();
                final String content = isFile ? null : filePointer.content;
                final String filename = filePointer.filename;

                ResourceLocator locator = new ResourceLocator(LocalhostConnector.getInstance(), isFile ? filename : "/");
                jsXPM.setLocator(locator);
                LOGGER.info("Script locator is %s", locator);

                if (isFile)
                    result = jsContext.evaluateReader(scope, new FileReader(filename), filename, 1, null);
                else
                    result = jsContext.evaluateString(scope, content, filename, 1, null);

            }

            if (result != null)
                LOGGER.debug("Returns %s", result.toString());
            else
                LOGGER.debug("Null result");

            StreamServer.sendReturnCode(packer, 0, result != null && result != Scriptable.NOT_FOUND &&
                    result != Undefined.instance ? result.toString() : "");

        } catch (Throwable e) {
            Throwable wrapped = e;
            LOGGER.info("Exception thrown there: %s", e.getStackTrace()[0]);
            while (wrapped.getCause() != null)
                wrapped = wrapped.getCause();

            LOGGER.printException(Level.INFO, wrapped);

            err.println(wrapped.toString());

            for (Throwable ee = e; ee != null; ee = ee.getCause()) {
                if (ee instanceof ContextualException) {
                    ContextualException ce = (ContextualException) ee;
                    List<String> context = ce.getContext();
                    if (!context.isEmpty()) {
                        err.format("%n[context]%n");
                        for (String s : ce.getContext()) {
                            err.format("%s%n", s);
                        }
                    }
                }
            }

            if (wrapped instanceof NotImplementedException)
                err.format("Line where the exception was thrown: %s", wrapped.getStackTrace()[0]);

            // Search for innermost rhino exception
            RhinoException rhinoException = null;
            for (Throwable t = e; t != null; t = t.getCause())
                if (t instanceof RhinoException)
                    rhinoException = (RhinoException) t;

            if (rhinoException != null) {
                err.append("\n" + rhinoException.getScriptStackTrace());
            } else {
                err.format("Internal error:%n");
                e.printStackTrace(err);
            }

            err.flush();
            try {
                StreamServer.sendReturnCode(packer, 1, errString.toString());
            } catch (IOException e2) {
                LOGGER.warn("Could not send error message");
            }

        } finally {
            // Exit context
            Context.exit();
        }


    }
}
