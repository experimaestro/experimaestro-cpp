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

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 *
 */
public class Scripting {
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
}
