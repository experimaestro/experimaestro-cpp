package net.bpiwowar.xpm.utils;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.*;

/**
 * A stream for serializing
 */
public class JsonSerializationInputStream extends InputStream {
    private final PipedInputStream inputStream;
    private final PipedOutputStream outputStream;
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * Creates an input stream for an object to serialize
     * @param object The object to serialize
     * @param builder The GSON builder to use
     * @return An input stream
     */
    static public JsonSerializationInputStream of(Object object, GsonBuilder builder) {
        return new JsonSerializationInputStream(out -> {
            try (JsonWriter writer = new JsonWriter(out)) {
                final Gson gson = builder.create();
                gson.toJson(object, object.getClass(), writer);
            }
        });
    }

    public JsonSerializationInputStream(ExceptionalConsumer<Writer> f) {
        outputStream = new PipedOutputStream();
        try {
            inputStream = new PipedInputStream(outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Start the writing thread
        new Thread(() -> {
            try (OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {
                f.apply(writer);
            } catch (IOException e) {
                LOGGER.warn(e, "I/O exception");
            } catch (Exception e) {
                throw new XPMRuntimeException(e);
            }
        }, "JsonSerializer").start();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
        outputStream.close();
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public long skip(long n) throws IOException {
        return inputStream.skip(n);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return inputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return inputStream.read(b, off, len);
    }

    @Override
    public synchronized void mark(int readlimit) {
        inputStream.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }


}
