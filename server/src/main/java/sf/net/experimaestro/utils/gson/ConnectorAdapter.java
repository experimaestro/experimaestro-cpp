package sf.net.experimaestro.utils.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import sf.net.experimaestro.connectors.Connector;

import java.io.IOException;
import java.sql.SQLException;

/**
 *
 */
public class ConnectorAdapter extends TypeAdapter<Connector> {
    @Override
    public void write(JsonWriter out, Connector connector) throws IOException {
        if (connector == null) {
            out.nullValue();
        } else {
            out.value(connector.getId());
        }
    }

    @Override
    public Connector read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            return null;
        }

        final String id = in.nextString();
        try {
            final Connector connector = Connector.findByURI(id);
            if (connector == null) {
                throw new IOException("Could not find the connector (not in DB)" + id);
            }
            return connector;
        } catch (SQLException e) {
            throw new IOException("Could not find the connector" + id, e);
        }
    }
}
