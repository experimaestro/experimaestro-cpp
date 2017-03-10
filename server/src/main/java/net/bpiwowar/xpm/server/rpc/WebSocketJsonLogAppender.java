package net.bpiwowar.xpm.server.rpc;

import net.bpiwowar.xpm.server.XPMWebSocketListener;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.appender.OutputStreamAppender;
import org.apache.logging.log4j.core.appender.OutputStreamManager;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Serializable;

/**
 * Appender for Web socket communications
 */
@Plugin(name = "JsonWebSocket", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public final class WebSocketJsonLogAppender extends AbstractAppender {

    /**
     * Instantiates a WriterAppender and set the output destination to a new {@link OutputStreamWriter}
     * initialized with <code>os</code> as its {@link OutputStream}.
     *
     * @param name   The name of the Appender.
     * @param layout The layout to format the message.
     * @param filter The filter
     */
    protected WebSocketJsonLogAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout);
    }


    @PluginFactory
    public static WebSocketJsonLogAppender createAppender(@PluginAttribute("name") String name,
                                                          @PluginElement("Layout") Layout layout,
                                                          @PluginElement("Filters") Filter filter) {
        return new WebSocketJsonLogAppender(name, filter, layout);
    }

    @Override
    public void append(LogEvent event) {
        final byte[] bytes = getLayout().toByteArray(event);

        if (bytes != null && bytes.length > 0) {
            try {
                XPMWebSocketListener.write(event, bytes);
            } catch (IOException ignored) {
                // Cannot do much
            }
        }
    }
}
