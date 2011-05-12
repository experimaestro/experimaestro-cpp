package sf.net.experimaestro.exceptions;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;

public class ExperimaestroException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	ArrayList<String> context = new ArrayList<String>();

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

	public void addContext(String string, Object... values) {
		context.add(format(string, values));
	}

	public List<String> getContext() {
		return context;
	}

}
