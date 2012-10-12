/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.manager;

import org.w3c.dom.Element;

import static java.lang.String.format;

/**
 * A qualified name that can be compared
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class QName implements Comparable<QName> {
	/**
	 * The URI
	 */
	String uri;
	
	/**
	 * The local name 
	 */
	String localName;
	
	/**
	 * Constructs with an URI and a local name
	 * @param uri
	 * @param localName
	 */
	public QName(String uri, String localName) {
		this.uri = uri;
		this.localName = localName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((localName == null) ? 0 : localName.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;

		if (getClass() != obj.getClass())
			return false;
		QName other = (QName) obj;
		if (localName == null) {
			if (other.localName != null)
				return false;
		} else if (!localName.equals(other.localName))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}


	@Override
	public int compareTo(QName other) {
		int z =  (uri != null ? 1 : 0) - (other.uri != null ? 1 : 0);
		if (z != 0 ) return z;
		
		z = uri.compareTo(other.uri);
		if (z != 0 ) return z;
		
		 z =  (localName != null ? 1 : 0) - (other.localName != null ? 1 : 0);
		if (z != 0 ) return z;

		return localName.compareTo(other.localName);
	}

	public String getNamespaceURI() {
		return uri;
	}

	public String getLocalPart() {
		return localName;
	}
	
	@Override
	public String toString() {
		if (uri == null)
			return localName;
		return format("{%s}%s", uri, localName);
	}


    public boolean sameQName(Element element) {
            return equals(new QName(element.getNamespaceURI(), element.getLocalName()));
    }
}
