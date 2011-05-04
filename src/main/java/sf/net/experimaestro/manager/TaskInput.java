package sf.net.experimaestro.manager;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import sf.net.experimaestro.exceptions.ExperimaestroException;

public class TaskInput extends Input {

	final TaskFactory factory;

	public TaskInput(TaskFactory factory, QName type) {
		super(type);
		this.factory = factory;
	}

	@Override
	public void setDefaultValue(Document defaultValue) {
		throw new ExperimaestroException(
				"Default value must not be set for task inputs");
	}

	@Override
	Value newValue() {
		return new TaskValue(this);
	}
}
