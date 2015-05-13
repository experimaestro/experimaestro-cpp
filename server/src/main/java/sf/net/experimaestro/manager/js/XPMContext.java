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

import bpiwowar.argparser.utils.Introspection;
import org.apache.log4j.Hierarchy;
import org.eclipse.wst.jsdt.debug.rhino.debugger.RhinoDebugger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import sf.net.experimaestro.connectors.ConnectorOptions;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.connectors.SSHOptions;
import sf.net.experimaestro.connectors.XPMConnector;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.Repositories;
import sf.net.experimaestro.manager.plans.Constant;
import sf.net.experimaestro.manager.scripting.ScriptContext;
import sf.net.experimaestro.manager.scripting.StaticContext;
import sf.net.experimaestro.scheduler.Command;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;

/**
 * Global context when executing a javascript
 */
public class XPMContext implements AutoCloseable {
    static private Scriptable XPM_SCOPE;

    static private final Logger LOGGER = Logger.getLogger();

    /**
     * The global functions
     */
    static final JSUtils.FunctionDefinition[] definitions = {
            new JSUtils.FunctionDefinition(XPMObject.class, "qname", Object.class, String.class),
            new JSUtils.FunctionDefinition(XPMObject.class, "include"),
            new JSUtils.FunctionDefinition(XPMObject.class, "include_repository"),
            new JSUtils.FunctionDefinition(XPMObject.class, "script_file"),
            new JSUtils.FunctionDefinition(XPMObject.class, "xpath", String.class, Object.class),
            new JSUtils.FunctionDefinition(XPMObject.class, "path"),
            new JSUtils.FunctionDefinition(XPMObject.class, "value"),
            new JSUtils.FunctionDefinition(XPMObject.class, "file"),
            new JSUtils.FunctionDefinition(XPMObject.class, "format"),
            new JSUtils.FunctionDefinition(XPMObject.class, "unwrap", Object.class),
            new JSUtils.FunctionDefinition(XPMObject.class, "set_workdir"),
    };

    private final RhinoDebugger debugger;
    private final Context context;
    private final Map<String, String> environment;
    private final Repositories repositories;
    private final StaticContext staticContext;

    /**
     * Get the scope where all the main objects are defined
     */
    synchronized public static Scriptable init() throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        if (XPM_SCOPE == null) {
            ContextFactory factory = new ContextFactory();
            Context context = factory.enterContext();
            // Sealed object
            XPM_SCOPE = context.initStandardObjects(null, true);


            // --- Define functions and classes

            // Define the new classes (scans the package for implementations of ScriptableObject)
            ArrayList<Class<?>> list = new ArrayList<>();

            try {
                final String packageName = XPMObject.class.getPackage().getName();
                final String resourceName = packageName.replace('.', '/');
                final Enumeration<URL> urls = XPMObject.class.getClassLoader().getResources(resourceName);
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    Introspection.addClasses(
                            aClass -> (ScriptableObject.class.isAssignableFrom(aClass)
                                    || JSConstructable.class.isAssignableFrom(aClass)
                                    || JSBaseObject.class.isAssignableFrom(aClass))
                                    && ((aClass.getModifiers() & Modifier.ABSTRACT) == 0), list, packageName, -1, url);
                }
            } catch (IOException e) {
                LOGGER.error(e, "While trying to grab resources");
            }

            // Add the classes to javascript
            for (Class<?> aClass : list) {
                JSBaseObject.defineClass(XPM_SCOPE, aClass);
            }

            String classname = null;
            try (InputStream in = XPMContext.class.getResource("/META-INF/sf.net.experimaestro.scripting.xml").openStream()) {
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document document = builder.parse(in);
                XPath xPath = XPathFactory.newInstance().newXPath();

                NodeList classes = (NodeList) xPath.compile("/scripting/classes/class").evaluate(document, XPathConstants.NODESET);
                for (int i = 0; i < classes.getLength(); ++i) {
                    classname = classes.item(i).getTextContent();
                    JSBaseObject.defineClass(XPM_SCOPE, XPMContext.class.getClassLoader().loadClass(classname));
                }
            } catch (IOException e) {
                throw new XPMRuntimeException("Cannot find scripting file");
            } catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
                throw new XPMRuntimeException(e, "Error with XML processing");
            } catch (ClassNotFoundException e) {
                throw new XPMRuntimeException(e, "Could not find class %s", classname);
            }


            // Add global functions
            for (JSUtils.FunctionDefinition definition : definitions)
                JSUtils.addFunction(XPM_SCOPE, definition);

            // namespace
            addNewObject(context, XPM_SCOPE, "xp", "Namespace", new Object[]{"xp", Manager.EXPERIMAESTRO_NS});

            // Adds a Pipe object
            XPMContext.addNewObject(context, XPM_SCOPE, "PIPE", "Pipe", new Object[]{});

            context.exit();
        }
        return XPM_SCOPE;
    }

    public XPMContext(Map<String, String> environment, Repositories repositories, Scheduler scheduler, Hierarchy loggerRepository, Integer debugPort) throws Exception {
        this.repositories = repositories;
        this.staticContext = new StaticContext(scheduler, loggerRepository).repository(repositories);
        // --- Debugging via JSDT
        // http://wiki.eclipse.org/JSDT/Debug/Rhino/Embedding_Rhino_Debugger#Example_Code
        ContextFactory factory = new ContextFactory();

        if (debugPort != null) {
            debugger = new RhinoDebugger("transport=socket,suspend=y,address=" + debugPort);
            debugger.start();
            factory.addListener(debugger);
        } else {
            debugger = null;
        }
        context = factory.enterContext();

        this.environment = environment;

    }

    static void addNewObject(Context cx, Scriptable scope,
                             final String objectName, final String className,
                             final Object[] params) {
        ScriptableObject.defineProperty(scope, objectName, cx.newObject(scope, className, params), 0);
    }

    @Override
    public void close() throws Exception {
        // Stop debugger
        if (debugger != null)
            try {
                debugger.stop();
            } catch (Exception e) {
                LOGGER.error(e);
            }
        // Exit context
        context.exit();

    }

    public Object evaluateReader(LocalhostConnector connector, Path locator, FileReader reader, String filename, int lineno, Object security) throws Exception {
        try(ScriptContext scriptContext =  staticContext.scriptContext()) {
            XPMObject xpmObject = getXPMObject(connector, locator, scriptContext);
            XPMObject.threadXPM.set(xpmObject);
            return context.evaluateReader(xpmObject.scope, reader, filename, lineno, security);
        }
    }

    public Object evaluateString(LocalhostConnector connector, Path locator, String content, String filename, int lineno, Object security) throws Exception {
        try(ScriptContext scriptContext =  staticContext.scriptContext()) {
            XPMObject xpmObject = getXPMObject(connector, locator, scriptContext);
            XPMObject.threadXPM.set(xpmObject);
            return context.evaluateString(xpmObject.scope, content, filename, lineno, security);
        }
    }

    private XPMObject getXPMObject(LocalhostConnector connector, Path locator, ScriptContext scriptContext)
            throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
            return new XPMObject(scriptContext, connector, locator, context, environment, newScope());
    }

    public static Scriptable newScope() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Scriptable newScope = Context.getCurrentContext().newObject(init());
        newScope.setPrototype(XPM_SCOPE);
        newScope.setParentScope(null);

        return newScope;
    }
}
