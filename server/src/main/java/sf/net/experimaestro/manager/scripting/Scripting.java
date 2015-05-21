package sf.net.experimaestro.manager.scripting;

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

import org.apache.commons.lang.mutable.MutableInt;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.scheduler.Command;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 */
public class Scripting {
    final static private Logger LOGGER = Logger.getLogger();
    public static MethodFunction[] FUNCTIONS;

    static {
        MutableInt errors = new MutableInt(0);
        FUNCTIONS = ClassDescription.analyzeClass(Functions.class).getMethods()
            .entrySet().stream().map(e -> {
                final MethodFunction methodFunction = new MethodFunction(e.getKey());
                for (Method method : e.getValue()) {
                    if ((method.getModifiers() & Modifier.STATIC) == 0) {
                        LOGGER.error("Method %s is not static", method);
                        errors.increment();
                    }
                }
                methodFunction.add(null, e.getValue());
                return methodFunction;
            }).toArray(n -> new MethodFunction[n]);


        if (errors.intValue() > 0) {
            throw new RuntimeException("Errors while initializing the list of scripting functions");
        }
    }

    public static void forEachType(Consumer<Class> f) {

        String classname = null;
        try (InputStream in = Scripting.class.getResource("/META-INF/sf.net.experimaestro.scripting.xml").openStream()) {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(in);
            XPath xPath = XPathFactory.newInstance().newXPath();

            NodeList classes = (NodeList) xPath.compile("/scripting/classes/class").evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < classes.getLength(); ++i) {
                classname = classes.item(i).getTextContent();
                final Class<?> aClass = ScriptContext.class.getClassLoader().loadClass(classname);
                f.accept(aClass);
            }
        } catch (IOException e) {
            throw new XPMRuntimeException("Cannot find scripting file");
        } catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
            throw new XPMRuntimeException(e, "Error with XML processing");
        } catch (ClassNotFoundException e) {
            throw new XPMRuntimeException(e, "Could not find class %s", classname);
        }
    }

    public static void forEachFunction(Consumer<MethodFunction> f) {
        Stream.of(FUNCTIONS).forEach(f);
    }

    public static void forEachConstant(BiConsumer<String, Object> f) {

        f.accept("PIPE", Command.Pipe.getInstance());
        f.accept("xp", Manager.EXPERIMAESTRO_NS_OBJECT);
    }
}
