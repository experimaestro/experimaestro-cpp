package net.bpiwowar.xpm.server.rpc;

import com.google.common.collect.ImmutableMap;
import net.bpiwowar.xpm.utils.log.DefaultFactory;
import net.bpiwowar.xpm.utils.log.Logger;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

import javax.servlet.http.HttpServlet;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

/**
 *
 */
public class BaseJsonRPCMethods extends HttpServlet implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger();
    protected final JSONRPCRequest mos;

    HashMap<String, BufferedWriter> writers = new HashMap<>();

    public BaseJsonRPCMethods(JSONRPCRequest mos) {
        this.mos = mos;
    }

    /**
     * Return the output stream for the request
     */
    protected BufferedWriter getRequestOutputStream() {
        return getRequestStream("out");
    }

    /**
     * Return the error stream for the request
     */
    protected BufferedWriter getRequestErrorStream() {
        return getRequestStream("err");
    }

    /**
     * Return a stream with the given ID
     */
    private BufferedWriter getRequestStream(final String id) {
        BufferedWriter bufferedWriter = writers.get(id);
        if (bufferedWriter == null) {
            bufferedWriter = new BufferedWriter(new Writer() {
                @Override
                public void write(char[] cbuf, int off, int len) throws IOException {
                    ImmutableMap<String, String> map = ImmutableMap.of("stream", id, "value", new String(cbuf, off, len));
                    mos.message(map);
                }

                @Override
                public void flush() throws IOException {
                }

                @Override
                public void close() throws IOException {
                    throw new UnsupportedOperationException();
                }
            });
            writers.put(id, bufferedWriter);
        }
        return bufferedWriter;
    }

    public Hierarchy getScriptLogger() {
//        final RootLogger root = new RootLogger(Level.INFO);
        final Logger root = new Logger("root");
        root.setLevel(Level.INFO);
        final Hierarchy loggerRepository = new Hierarchy(root) {
            public Logger getLogger(String name) {
                return (Logger) this.getLogger(name, new DefaultFactory());
            }
        };
        BufferedWriter stringWriter = getRequestErrorStream();

        PatternLayout layout = new PatternLayout("%-6p [%c] %m%n");
        WriterAppender appender = new WriterAppender(layout, stringWriter);
        root.addAppender(appender);
        return loggerRepository;
    }

    @Override
    public void close() {
        writers.values().forEach(bufferedWriter -> {
            try {
                bufferedWriter.close();
            } catch (IOException e) {
                LOGGER.error(e, "Cannot close RPC output stream");
            }
        });
    }
}
