package net.bpiwowar.xpm.manager.scripting;

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

import net.bpiwowar.xpm.commands.Pipe;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.Constants;
import net.bpiwowar.xpm.utils.log.Logger;
import org.apache.commons.lang.mutable.MutableInt;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 */
public class Scripting {
    final static private Logger LOGGER = Logger.getLogger();
    public static MethodFunction[] FUNCTIONS = getMethodFunctions(Functions.class);

    public static MethodFunction[] getMethodFunctions(Class<?> aClass) {
        MutableInt errors = new MutableInt(0);
        final MethodFunction[] methodFunctions = ClassDescription.analyzeClass(aClass).getMethods()
                .values().stream().toArray(n -> new MethodFunction[n]);
        if (errors.intValue() > 0) {
            throw new RuntimeException("Errors while initializing the list of scripting functions");
        }
        return methodFunctions;
    }

    static List<Class<?>> TYPES;
    static HashMap<String, ClassDescription> CLASSES;

    /**
     * List all types
     *
     * @param f The callback function
     */
    public static void forEachType(Consumer<Class> f) {
        getTypes().forEach(f);
    }

    public static List<Class<?>> getTypes() {
        init();
        return TYPES;
    }

    public static void init() {
        if (TYPES == null) {
            TYPES = new ArrayList<>();
            CLASSES = new HashMap<>();
            String classname = null;
            try (InputStream in = Scripting.class.getResource("/META-INF/net.bpiwowar.xpm.scripting.xml").openStream()) {
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document document = builder.parse(in);
                XPath xPath = XPathFactory.newInstance().newXPath();

                NodeList classes = (NodeList) xPath.compile("/scripting/classes/class").evaluate(document, XPathConstants.NODESET);
                for (int i = 0; i < classes.getLength(); ++i) {
                    classname = classes.item(i).getTextContent();
                    final Class<?> aClass = Context.class.getClassLoader().loadClass(classname);
                    TYPES.add(aClass);
                    final ClassDescription classDescription = ClassDescription.analyzeClass(aClass);
                    final String className = classDescription.getClassName();
                    if (className == null || className.isEmpty())
                        throw new AssertionError("Class names of registered types should not be null or empty");
                    CLASSES.put(className, classDescription);
                }
            } catch (IOException e) {
                throw new XPMRuntimeException("Cannot find scripting file");
            } catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
                throw new XPMRuntimeException(e, "Error with XML processing");
            } catch (ClassNotFoundException e) {
                throw new XPMRuntimeException(e, "Could not find class %s", classname);
            }
        }
    }

    public static void forEachFunction(Consumer<MethodFunction> f) {
        Stream.of(FUNCTIONS).forEach(f);
    }

    public static void forEachConstant(BiConsumer<String, Object> f) {
        f.accept("PIPE", Pipe.getInstance());
        f.accept("xp", Constants.EXPERIMAESTRO_NS_OBJECT);
    }

    public static void forEachObject(BiConsumer<String, Object> f) {
//        f.accept("logger", new ScriptingLogger("xpm"));
//        f.accept("tasks", new Tasks());
        f.accept("xpm", new XPM());
    }

    public static ClassDescription getClassDescription(String classname) {
        init();
        return CLASSES.get(classname);
    }
}
