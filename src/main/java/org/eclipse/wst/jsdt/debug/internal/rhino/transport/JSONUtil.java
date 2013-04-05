/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.internal.rhino.transport;

import java.math.BigDecimal;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Undefined;


/**
 * This class provides utilities for working with JSON.
 * <br><br>JSON identifiers map to Java as follows:
 * <ul>
 * <li>string	<--> 	java.lang.String</li>
 * <li>number	<--> 	java.math.Number (BigDecimal)</li>
 * <li>object	<--> 	java.util.Map (HashMap)</li>
 * <li>array	<--> 	java.util.Collection (ArrayList)</li>
 * <li>true		<--> 	java.lang.Boolean.TRUE</li>
 * <li>false	<--> 	java.lang.Boolean.FALSE</li>
 * <li>null		<--> 	null</li>
 * </ul> 
 * 
 * @see JSONConstants
 * @since 1.0
 */
public final class JSONUtil {

	/**
	 * Constructor
	 * no instantiation
	 */
	private JSONUtil() {}
	
	/**
	 * Reads an object from the given JSON string.
	 * <br><br>JSON identifiers map to Java as follows:
	 * <ul>
	 * <li>string	<--> 	java.lang.String</li>
	 * <li>number	<--> 	java.math.Number (BigDecimal)</li>
	 * <li>object	<--> 	java.util.Map (HashMap)</li>
	 * <li>array	<--> 	java.util.Collection (ArrayList)</li>
	 * <li>true		<--> 	java.lang.Boolean.TRUE</li>
	 * <li>false	<--> 	java.lang.Boolean.FALSE</li>
	 * <li>null		<--> 	null</li>
	 * </ul> 
	 * @param jsonString
	 * @return the object corresponding to the JSON string or <code>null</code>
	 */
	public static Object read(String jsonString) {
		return parse(new StringCharacterIterator(jsonString));
	}

	/**
	 * Writes the given object to JSON
	 * <br><br>JSON identifiers map to Java as follows:
	 * <ul>
	 * <li>string	<--> 	java.lang.String</li>
	 * <li>number	<--> 	java.math.Number (BigDecimal)</li>
	 * <li>object	<--> 	java.util.Map (HashMap)</li>
	 * <li>array	<--> 	java.util.Collection (ArrayList)</li>
	 * <li>true		<--> 	java.lang.Boolean.TRUE</li>
	 * <li>false	<--> 	java.lang.Boolean.FALSE</li>
	 * <li>null		<--> 	null</li>
	 * </ul> 
	 * @param jsonObject
	 * @return the composed JSON string, never <code>null</code>
	 */
	public static String write(Object jsonObject) {
		StringBuffer buffer = new StringBuffer();
		writeValue(jsonObject, buffer);
		return buffer.toString();
	}

	/**
	 * Creates an {@link IllegalStateException} for the given message and iterator
	 * 
	 * @param message the message for the exception
	 * @param it the iterator
	 * @return a new {@link IllegalStateException} 
	 */
	private static RuntimeException error(String message, CharacterIterator it) {
		return new IllegalStateException("[" + it.getIndex() + "] " + message); //$NON-NLS-1$//$NON-NLS-2$
	}

	/**
	 * Creates an {@link IllegalStateException} for the given message
	 * 
	 * @param message the message for the exception
	 * @return a new {@link IllegalStateException}
	 */
	private static RuntimeException error(String message) {
		return new IllegalStateException(message);
	}

	/**
	 * Parses the object value from the JSON string.
	 * <br><br>JSON identifiers map to Java as follows:
	 * <ul>
	 * <li>string	<--> 	java.lang.String</li>
	 * <li>number	<--> 	java.math.Number (BigDecimal)</li>
	 * <li>object	<--> 	java.util.Map (HashMap)</li>
	 * <li>array	<--> 	java.util.Collection (ArrayList)</li>
	 * <li>true		<--> 	java.lang.Boolean.TRUE</li>
	 * <li>false	<--> 	java.lang.Boolean.FALSE</li>
	 * <li>null		<--> 	null</li>
	 * </ul> 
	 * @param it
	 * @return
	 */
	private static Object parse(CharacterIterator it) {
		parseWhitespace(it);
		Object result = parseValue(it);
		parseWhitespace(it);

		if (it.current() != CharacterIterator.DONE) {
			throw error("should be done", it); //$NON-NLS-1$
		}
		return result;
	}

	/**
	 * Chews up whitespace from the iterator
	 * 
	 * @param it
	 */
	private static void parseWhitespace(CharacterIterator it) {
		char c = it.current();
		while (Character.isWhitespace(c)) {
			c = it.next();
		}
	}

	/**
	 * Delegate method that calls the correct parse* method for object creation
	 * <br><br>JSON identifiers map to Java as follows:
	 * <ul>
	 * <li>string	<--> 	java.lang.String</li>
	 * <li>number	<--> 	java.math.Number (BigDecimal)</li>
	 * <li>object	<--> 	java.util.Map (HashMap)</li>
	 * <li>array	<--> 	java.util.Collection (ArrayList)</li>
	 * <li>true		<--> 	java.lang.Boolean.TRUE</li>
	 * <li>false	<--> 	java.lang.Boolean.FALSE</li>
	 * <li>null		<--> 	null</li>
	 * </ul> 
	 * @param it
	 * @return
	 */
	private static Object parseValue(CharacterIterator it) {
		switch (it.current()) {
			case '{' :
				return parseObject(it);
			case '[' :
				return parseArray(it);
			case '"' :
				return parseString(it);
			case '-' :
			case '0' :
			case '1' :
			case '2' :
			case '3' :
			case '4' :
			case '5' :
			case '6' :
			case '7' :
			case '8' :
			case '9' :
				return parseNumber(it);
			case 't' :
				parseText(Boolean.TRUE.toString(), it);
				return Boolean.TRUE;
			case 'f' :
				parseText(Boolean.FALSE.toString(), it);
				return Boolean.FALSE;
			case 'n' :
				parseText(JSONConstants.NULL, it);
				return null;
			case 'u':
				parseText(JSONConstants.UNDEFINED, it);
				return null;
		}
		throw error("Bad JSON starting character '" + it.current() + "'", it); //$NON-NLS-1$ //$NON-NLS-2$;
	}

	/**
	 * Parses an {@link Map} object from the iterator or throws an
	 * {@link IllegalStateException} if parsing fails.
	 * 
	 * @param it
	 * @return a new {@link Map} object, never <code>null</code>
	 */
	private static Map parseObject(CharacterIterator it) {
		it.next();
		parseWhitespace(it);
		if (it.current() == '}') {
			it.next();
			return Collections.EMPTY_MAP;
		}

		Map map = new HashMap();
		while (true) {
			if (it.current() != '"')
				throw error("expected a string start '\"' but was '" + it.current() + "'", it); //$NON-NLS-1$ //$NON-NLS-2$
			String key = parseString(it);
			if (map.containsKey(key))
				throw error("' already defined" + "key '" + key, it); //$NON-NLS-1$ //$NON-NLS-2$
			parseWhitespace(it);
			if (it.current() != ':')
				throw error("expected a pair separator ':' but was '" + it.current() + "'", it); //$NON-NLS-1$ //$NON-NLS-2$
			it.next();
			parseWhitespace(it);
			Object value = parseValue(it);
			map.put(key, value);
			parseWhitespace(it);
			if (it.current() == ',') {
				it.next();
				parseWhitespace(it);
				continue;
			}

			if (it.current() != '}')
				throw error("expected an object close '}' but was '" + it.current() + "'", it); //$NON-NLS-1$ //$NON-NLS-2$
			break;
		}
		it.next();
		return map;
	}

	/**
	 * Parses an {@link ArrayList} from the given iterator or throws an
	 * {@link IllegalStateException} if parsing fails
	 * 
	 * @param it
	 * @return a new {@link ArrayList} object never <code>null</code>
	 */
	private static List parseArray(CharacterIterator it) {
		it.next();
		parseWhitespace(it);
		if (it.current() == ']') {
			it.next();
			return Collections.EMPTY_LIST;
		}

		List list = new ArrayList();
		while (true) {
			Object value = parseValue(it);
			list.add(value);
			parseWhitespace(it);
			if (it.current() == ',') {
				it.next();
				parseWhitespace(it);
				continue;
			}

			if (it.current() != ']')
				throw error("expected an array close ']' but was '" + it.current() + "'", it); //$NON-NLS-1$ //$NON-NLS-2$
			break;
		}
		it.next();
		return list;
	}

	private static void parseText(String string, CharacterIterator it) {
		int length = string.length();
		char c = it.current();
		for (int i = 0; i < length; i++) {
			if (c != string.charAt(i))
				throw error("expected to parse '" + string + "' but character " + (i + 1) + " was '" + c + "'", it); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$;
			c = it.next();
		}
	}

	private static Object parseNumber(CharacterIterator it) {
		StringBuffer buffer = new StringBuffer();
		char c = it.current();
		while (Character.isDigit(c) || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
			buffer.append(c);
			c = it.next();
		}
		try {
			return new BigDecimal(buffer.toString());
		} catch (NumberFormatException e) {
			throw error("expected a number but was '" + buffer.toString() + "'", it); //$NON-NLS-1$ //$NON-NLS-2$;
		}
	}

	private static String parseString(CharacterIterator it) {
		char c = it.next();
		if (c == '"') {
			it.next();
			return ""; //$NON-NLS-1$
		}
		StringBuffer buffer = new StringBuffer();
		while (c != '"') {
			if (Character.isISOControl(c)) {
				//XXX we should ignore the ISO control chars and make a best effort to continue
				c = it.next();
				continue;
				//throw error("illegal iso control character: '" + Integer.toHexString(c) + "'", it); //$NON-NLS-1$ //$NON-NLS-2$);
			}
			if (c == '\\') {
				c = it.next();
				switch (c) {
					case '"' :
					case '\\' :
					case '/' :
						buffer.append(c);
						break;
					case 'b' :
						buffer.append('\b');
						break;
					case 'f' :
						buffer.append('\f');
						break;
					case 'n' :
						buffer.append('\n');
						break;
					case 'r' :
						buffer.append('\r');
						break;
					case 't' :
						buffer.append('\t');
						break;
					case 'u' :
						StringBuffer unicode = new StringBuffer(4);
						for (int i = 0; i < 4; i++) {
							unicode.append(it.next());
						}
						try {
							buffer.append((char) Integer.parseInt(unicode.toString(), 16));
						} catch (NumberFormatException e) {
							throw error("expected a unicode hex number but was '" + unicode.toString() + "'", it); //$NON-NLS-1$ //$NON-NLS-2$););
						}
						break;
					default :
						throw error("illegal escape character '" + c + "'", it); //$NON-NLS-1$ //$NON-NLS-2$););
				}
			} else
				buffer.append(c);

			c = it.next();
		}
		c = it.next();
		return buffer.toString();
	}

	private static void writeValue(Object value, StringBuffer buffer) {
		if (value == null)
			buffer.append(JSONConstants.NULL);
		else if (value instanceof Boolean)
			buffer.append(value.toString());
		else if (value instanceof Number)
			writeNumber((Number) value, buffer);
		else if (value instanceof CharSequence)
			writeCharSequence((CharSequence) value, buffer);
		else if (value instanceof Collection)
			writeArray((Collection) value, buffer);
		else if (value instanceof Map)
			writeObject((Map) value, buffer);
		else  if(value instanceof Undefined) {
			buffer.append(JSONConstants.UNDEFINED);
		}
		else
			throw error("Unexpected object instance type was '" + value.getClass().getName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$););
	}

	private static void writeNumber(Number value, StringBuffer buffer) {
		if (value instanceof Double) {
			if (((Double)value).isNaN() || ((Double)value).isInfinite()) {
				buffer.append(JSONConstants.NULL);
				return;
			}
		} else if (value instanceof Float) {
			if (((Float)value).isNaN() || ((Float)value).isInfinite()) {
				buffer.append(JSONConstants.NULL);
				return;
			}
		}
		buffer.append(value.toString());
	}

	private static void writeObject(Map map, StringBuffer buffer) {
		buffer.append('{');
		for (Iterator iterator = map.keySet().iterator(); iterator.hasNext();) {
			Object key = iterator.next();
			if (!(key instanceof String))
				throw error("Map keys must be an instance of String but was '" + key.getClass().getName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$););
			writeCharSequence((CharSequence) key, buffer);
			buffer.append(':');
			writeValue(map.get(key), buffer);
			buffer.append(',');
		}
		if (buffer.charAt(buffer.length() - 1) == ',')
			buffer.setCharAt(buffer.length() - 1, '}');
		else
			buffer.append('}');
	}

	private static void writeArray(Collection collection, StringBuffer buffer) {
		buffer.append('[');
		for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
			writeValue(iterator.next(), buffer);
			buffer.append(',');
		}
		if (buffer.charAt(buffer.length() - 1) == ',')
			buffer.setCharAt(buffer.length() - 1, ']');
		else
			buffer.append(']');
	}

	private static void writeCharSequence(CharSequence string, StringBuffer buffer) {
		buffer.append('"');
		int length = string.length();
		for (int i = 0; i < length; i++) {
			char c = string.charAt(i);
			switch (c) {
				case '"' :
				case '\\' :
				case '/' :
					buffer.append('\\');
					buffer.append(c);
					break;
				case '\b' :
					buffer.append("\\b"); //$NON-NLS-1$
					break;
				case '\f' :
					buffer.append("\\f"); //$NON-NLS-1$
					break;
				case '\n' :
					buffer.append("\\n"); //$NON-NLS-1$
					break;
				case '\r' :
					buffer.append("\\r"); //$NON-NLS-1$
					break;
				case '\t' :
					buffer.append("\\t"); //$NON-NLS-1$
					break;
				default :
					if (Character.isISOControl(c)) {
						buffer.append("\\u"); //$NON-NLS-1$
						String hexString = Integer.toHexString(c);
						for (int j = hexString.length(); j < 4; j++)
							buffer.append('0');
						buffer.append(hexString);
					} else
						buffer.append(c);
			}
		}
		buffer.append('"');
	}
}
