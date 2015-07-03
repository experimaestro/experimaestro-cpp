package sf.net.experimaestro.connectors;

import java.io.OutputStream;

/**
 *
 */
interface StreamSetter {
    void setStream(OutputStream out, boolean dontClose);

    int streamNumber();
}
