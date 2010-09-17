package bpiwowar.expmanager.locks;

import static java.lang.String.format;

public class UnlockableException extends Exception {
	public UnlockableException() {
	}

	public UnlockableException(String format, Object... args) {
		super(format(format, args));
	}

	private static final long serialVersionUID = 1L;

}
