package sf.net.experimaestro.exceptions;

public class ExperimaestroException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ExperimaestroException() {
		super();
	}

	public ExperimaestroException(String message, Throwable t) {
		super(message, t);
	}

	public ExperimaestroException(Throwable t, String format, Object... values) {
		super(String.format(format, values), t);
	}

	public ExperimaestroException(String message) {
		super(message);
	}

	public ExperimaestroException(String format, Object... values) {
		super(String.format(format, values));
	}

	public ExperimaestroException(Throwable t) {
		super(t);
	}

}
