package net.bpiwowar.experimaestro.tasks;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.IOException;

/**
 * Created by bpiwowar on 7/10/14.
 */
abstract public class ReaderTypeAdapter<T> extends TypeAdapter<T> {
    @Override
    public void write(JsonWriter out, T value) throws IOException {
        throw new AssertionError("Cannot be used for serializing");
    }
}
