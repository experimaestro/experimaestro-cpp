package sf.net.experimaestro.utils.log;

import org.apache.log4j.Layout;
import org.apache.log4j.WriterAppender;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 *
 */
public class Router extends WriterAppender {
    final static ThreadLocal<Writer> threadOutput = new ThreadLocal<>();
    final static private Writer ERROR_STREAM_WRITER = new OutputStreamWriter(System.err);


    public Router() {
    }

    public Router(Layout layout) {
        this(layout, "System.out");
    }

    public Router(Layout layout, String target) {
        this.setLayout(layout);
        this.activateOptions();
    }

    @Override
    public void activateOptions() {
        super.activateOptions();
        this.setWriter(new RouterWriter());
    }

    final static private Writer writer() {
        Writer stream = threadOutput.get();
        if (stream == null) {
            return ERROR_STREAM_WRITER;
        }
        return stream;
    }

    final static public void writer(Writer writer) {
        threadOutput.set(writer);
    }

    private static class RouterWriter extends Writer {
        public RouterWriter() {
        }

        public void close() {
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            writer().write(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException {
            writer().flush();
        }
    }
}
