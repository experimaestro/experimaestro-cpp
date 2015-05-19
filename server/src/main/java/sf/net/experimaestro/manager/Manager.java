package sf.net.experimaestro.manager;

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

import org.apache.commons.lang.NotImplementedException;
import org.w3c.dom.Element;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.manager.scripting.XPM;
import sf.net.experimaestro.utils.MessageDigestWriter;
import sf.net.experimaestro.utils.XMLUtils;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import static java.lang.String.format;

/**
 * Container for global definitions
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Manager {

    public static final String EXPERIMAESTRO_NS = "http://experimaestro.lip6.fr";
    public static final QName XP_ARRAY = new QName(EXPERIMAESTRO_NS, "array");
    public static final QName XP_OBJECT = new QName(EXPERIMAESTRO_NS, "object");
    public static final QName XP_ANY = new QName(EXPERIMAESTRO_NS, "any");
    public static final Map<String, String> PREDEFINED_PREFIXES = new TreeMap<>();
    public static final String EXPERIMAESTRO_PREFIX = "xp";
    public static final QName XP_TYPE = new QName(null, "$type");

    /**
     * The simple value of the object
     */
    public static final QName XP_VALUE = new QName(null, "$value");

    /**
     * The resource associated with the object
     */
    public static final QName XP_RESOURCE = new QName(null, "$resource");

    /// Ignored value
    public static final QName XP_IGNORE = new QName(null, "$ignore");
    public static final String XPM_SIGNATURE = ".xpm-signature";

    static {
        PREDEFINED_PREFIXES.put("xp", EXPERIMAESTRO_NS);
    }

    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    /**
     * Get the namespaces (default and element based)
     *
     * @param element
     */
    public static Map<String, String> getNamespaces(Element element) {
        TreeMap<String, String> map = new TreeMap<>();
        for (Entry<String, String> mapping : PREDEFINED_PREFIXES.entrySet())
            map.put(mapping.getKey(), mapping.getValue());
        for (Entry<String, String> mapping : XMLUtils.getNamespaces(element))
            map.put(mapping.getKey(), mapping.getValue());
        return map;
    }

    /**
     * Wrap a node into a JSON object
     *
     * @param object
     * @return
     */
    public static Json wrap(Object object) {
        if (object instanceof Json)
            return (Json) object;

        throw new NotImplementedException();

//        Document document = XMLUtils.newDocument();
//        if (object instanceof Node) {
//            Node node = (Node) object;
//            switch (node.getNodeType()) {
//                case Node.ELEMENT_NODE:
//                    document.appendChild(document.adoptNode(node.cloneNode(true)));
//                    break;
//
//                case Node.TEXT_NODE:
//                case Node.ATTRIBUTE_NODE:
//                    Element element = document.createElementNS(EXPERIMAESTRO_NS, "value");
//                    element.setAttributeNS(EXPERIMAESTRO_NS, "value", node.getTextContent());
//                    document.appendChild(element);
//                    break;
//
//                case Node.DOCUMENT_FRAGMENT_NODE:
//                    Iterable<Element> elements = XMLUtils.elements(node.getChildNodes());
//                    int size = Iterables.size(elements);
//                    if (size == 1) {
//                        document.appendChild(document.adoptNode(Iterables.get(elements, 0).cloneNode(true)));
//                        break;
//                    }
//
//                    throw new NotImplementedException(String.format("Cannot convert a fragment with %d children", size));
//
//                default:
//                    throw new NotImplementedException("Cannot convert " + node.getClass() + " into a document");
//            }
//
//        } else {
//            Iterable<? extends Node> elements = XMLUtils.iterable((NodeList) object);
//            int size = Iterables.size(elements);
//            if (size == 1) {
//                document.appendChild(document.adoptNode(Iterables.get(elements, 0).cloneNode(true)));
//            } else
//                throw new NotImplementedException(String.format("Cannot convert a fragment with %d children", size));
//        }
//
//        return document;
    }

    static public Path uniqueDirectory(Path basedir, String prefix, QName id, Json jsonValues) throws IOException, NoSuchAlgorithmException {

        JsonObject json = new JsonObject();
        json.put("task", id.toString());
        json.put("value", jsonValues);

        String digest = getDigest(json);

        Path uniquePath = basedir.resolve(format("%s/%s", prefix, digest));

        Files.createDirectories(uniquePath);

        // Create the signature
        Path signature = uniquePath.resolve(Manager.XPM_SIGNATURE);
        String descriptor = getDescriptor(json);

        if (!Files.exists(signature)) {
            // Write the signature in the
            try (PrintWriter writer = new PrintWriter(Files.newOutputStream(signature))) {
                writer.write(descriptor);
            }
        } else {
            // Check that the signature is the same
            // @TODO more efficient comparison by avoiding to compute the whole signature
            char buffer[] = new char[1024];
            int offset = 0;
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(signature))) {
                int read;
                while ((read = reader.read(buffer)) > 0) {
                    if (offset + read > descriptor.length()) {
                        throw new RuntimeException("Signature JSON do not match in " + signature.toString());
                    }

                    for (int i = 0; i < read; ++i) {
                        if (buffer[i] != descriptor.charAt(offset + i)) {
                            throw new XPMRuntimeException("Signature JSON do not match in %s: at offset %d, %s <> %s",
                                    signature.toString(), i + offset, buffer[i], descriptor.charAt(i + offset));
                        }
                    }

                    offset += read;

                }

            }
        }

        return uniquePath;
    }

    public static String getDescriptor(Json json) throws IOException {
        StringWriter writer = new StringWriter();
        json.writeDescriptorString(writer);
        return writer.getBuffer().toString();
    }

    /**
     * Get the hash of a given json
     */
    public static String getDigest(Json json) throws NoSuchAlgorithmException, IOException {
        // Other options: SHA-256, SHA-512
        MessageDigestWriter writer = new MessageDigestWriter(Charset.forName("UTF-8"), "MD5");
        json.writeDescriptorString(writer);
        writer.close();

        return DatatypeConverter.printHexBinary(writer.getDigest()).toLowerCase();
    }
}