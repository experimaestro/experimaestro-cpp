package sf.net.experimaestro.manager.python;

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

import org.apache.log4j.Hierarchy;
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.Repositories;
import sf.net.experimaestro.manager.scripting.*;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

import static java.lang.String.format;

/**
 * Global context when executing a javascript
 */
public class PythonContext implements AutoCloseable {

    static private final Logger LOGGER = Logger.getLogger();
    private final Map<String, String> environment;
    private final StaticContext staticContext;
    private final PythonInterpreter interpreter;
    private final ScriptContext scriptContext;

    public PythonContext(Map<String, String> environment, Repositories repositories, Scheduler scheduler,
                         Hierarchy loggerRepository,
                         BufferedWriter out, BufferedWriter err) throws Exception {
        this.staticContext = new StaticContext(scheduler, loggerRepository)
                .repository(repositories);
        this.environment = environment;
        interpreter = new PythonInterpreter();
        interpreter.setOut(out);
        interpreter.setErr(err);

        // Add common methods
        Map<String, ArrayList<Method>> scriptingFunctionsMap = ClassDescription.analyzeClass(Functions.class).getMethods();
        final Functions scriptingFunctions = new Functions();
        for (Map.Entry<String, ArrayList<Method>> entry : scriptingFunctionsMap.entrySet()) {
            MethodFunction function = new MethodFunction(entry.getKey());
            function.add(scriptingFunctions, entry.getValue());
            interpreter.set(entry.getKey(), new PythonMethod(function));
        }


        // Add classes
        String classname = null;
        try (InputStream in = PythonContext.class.getResource("/META-INF/sf.net.experimaestro.scripting.xml").openStream()) {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(in);
            XPath xPath = XPathFactory.newInstance().newXPath();

            NodeList classes = (NodeList) xPath.compile("/scripting/classes/class").evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < classes.getLength(); ++i) {
                classname = classes.item(i).getTextContent();
                final Class<?> aClass = ScriptContext.class.getClassLoader().loadClass(classname);
                final PythonType type = new PythonType(aClass);
                interpreter.set(type.getName(), type);
            }
        } catch (IOException e) {
            throw new XPMRuntimeException("Cannot find scripting file");
        } catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
            throw new XPMRuntimeException(e, "Error with XML processing");
        } catch (ClassNotFoundException e) {
            throw new XPMRuntimeException(e, "Could not find class %s", classname);
        }
        scriptContext = staticContext.scriptContext();
    }

    public static PyObject wrap(Object object) {
        // Simple case
        if (object == null)
            return Py.None;

        if (object instanceof PyObject)
            return (PyObject) object;


        // Simple types
        if (object instanceof String) {
            return new PyString((String) object);
        }

        // Exposed objects
        final Exposed exposed = object.getClass().getAnnotation(Exposed.class);
        if (exposed != null) {
            return new PythonObject(object);
        }

        throw new IllegalArgumentException(format("Cannot wrap class %s into python object", object.getClass()));
    }

    public static Object[] unwrap(PyObject[] args) {
        Object[] unwrapped = new Object[args.length];
        for (int i = 0; i < args.length; ++i) {
            unwrapped[i] = args[i].__tojava__(Object.class);
        }
        return unwrapped;
    }

    @Override
    public void close() throws Exception {
        scriptContext.close();
    }

    public Object evaluateReader(LocalhostConnector connector, Path locator, FileReader reader, String filename, int lineno, Object security) throws Exception {
        final PyCode code = interpreter.compile(reader, filename);
        return interpreter.eval(code);
    }

    public Object evaluateString(LocalhostConnector connector, Path locator, String content, String name, int lineno, Object security) throws Exception {
        final PyCode code = interpreter.compile(content, name);
        return interpreter.eval(code);
    }
}
