package sf.net.experimaestro.manager;

import javax.xml.namespace.QName;

public class Type {
	private static final long serialVersionUID = 1L;

	/**
	 * The qualified name of this type
	 */
	private final QName qName;

	public Type(QName qName) {
		this.qName = qName;
	}

	public QName getId() {
		return qName;
	}

}
