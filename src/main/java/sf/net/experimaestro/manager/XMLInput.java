package sf.net.experimaestro.manager;

import javax.xml.namespace.QName;

public class XMLInput extends Input {

	public XMLInput(QName type) {
		super(type);
	}

	@Override
	Value newValue() {
		return new XMLValue(this);
	}

}
