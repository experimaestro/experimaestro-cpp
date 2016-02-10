package net.bpiwowar.xpm.exceptions;

import java.io.IOException;

/**
 *
 */
public class WrappedIOException extends WrappedException {
    public WrappedIOException(IOException e) {
        super(e);
    }
}
