package sf.net.experimaestro.manager;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import sf.net.experimaestro.exceptions.ExperimaestroException;

public class TaskInput extends Input {

	private final TaskFactory factory;

	public TaskInput(TaskFactory factory, QName type) {
		super(type);
		this.factory = factory;
	}
	
	@Override
	public void setDefaultValue(Document defaultValue) {
		throw new ExperimaestroException("Default value must not be set for task inputs");
	}

	@Override
	Value newValue() {
		return new TaskValue();
	}

	public class TaskValue extends Value {
		private Task task;
		private Document value;

		public TaskValue() {
			super(TaskInput.this);
			task = TaskInput.this.factory.create();
		}

		@Override
		public void set(DotName id, Document value) {
			task.setParameter(id, value);
		}

		@Override
		public void process() {
			value = task.run();
		}

		@Override
		public Document get() {
			return value;
		}

	}
}
