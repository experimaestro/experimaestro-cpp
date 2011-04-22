package sf.net.experimaestro.manager;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

public class TaskInput extends Input {

	private final TaskFactory factory;

	public TaskInput(TaskFactory factory, QName type, boolean optional,
			String documentation) {
		super(type, optional, documentation);
		this.factory = factory;
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
