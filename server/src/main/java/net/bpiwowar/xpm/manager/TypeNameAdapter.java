package net.bpiwowar.xpm.manager;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * JSON adapter for TypeName
 */
public class TypeNameAdapter extends TypeAdapter<TypeName> {
    @Override
    public void write(JsonWriter out, TypeName value) throws IOException {
        out.value(value.toString());
    }

    @Override
    public TypeName read(JsonReader in) throws IOException {
        return TypeName.parse(in.nextString());
    }
}
