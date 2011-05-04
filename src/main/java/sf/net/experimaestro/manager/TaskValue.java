package sf.net.experimaestro.manager;

import org.w3c.dom.Document;

public class TaskValue extends Value {
	private Task task;
	private Document value;

	public TaskValue() {
	}

	public TaskValue(TaskInput taskInput) {
		super(taskInput);
		task = taskInput.factory.create();
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

	@Override
	protected void init(Value _other) {
		TaskValue other = (TaskValue) _other;
		super.init(other);
		task = other.task.copy();
	}

}