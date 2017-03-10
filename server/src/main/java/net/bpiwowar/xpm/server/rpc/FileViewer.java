package net.bpiwowar.xpm.server.rpc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

import static java.lang.Math.max;
import static java.lang.String.format;

/**
 *
 */
public class FileViewer implements AutoCloseable {
    final static private Logger LOGGER = LogManager.getFormatterLogger();

    private SeekableByteChannel channel;
    private final String uri;

    public FileViewer(String uri) throws IOException {
        try {
            channel = Files.newByteChannel(Paths.get(URI.create(uri)));
        } catch(NoSuchFileException e) {
            LOGGER.warn("File not found: %s", uri);
        }
        this.uri = uri;
    }

    @Override
    public String toString() {
        return format("FileViewer(%s)", uri);
    }

    synchronized public void close() throws IOException {
        if (channel != null) {
            channel.close();
            channel = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (channel != null) {
            LOGGER.warn("File %s was not closed", uri);
            close();
        }
    }

    synchronized public ByteBuffer read(long position, int size) throws IOException {
        if (channel != null) {
            if (position < 0) {
                position = max(0, channel.size() + position);
            }

            channel.position(position);
            final ByteBuffer byteBuffer = ByteBuffer.allocate(size);
            channel.read(byteBuffer);
            return byteBuffer;
        }

        return ByteBuffer.allocate(0);
    }
}
