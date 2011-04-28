package sf.net.experimaestro.exceptions;

public class NoSuchParameter extends ExperimaestroException {
	private static final long serialVersionUID = 1L;

	public NoSuchParameter() {
		super();
	}

	public NoSuchParameter(String format, Object... values) {
		super(format, values);
	}

	public NoSuchParameter(String message, Throwable t) {
		super(message, t);
	}

	public NoSuchParameter(String message) {
		super(message);
	}

	public NoSuchParameter(Throwable t, String format, Object... values) {
		super(t, format, values);
	}

	public NoSuchParameter(Throwable t) {
		super(t);
	}

}
