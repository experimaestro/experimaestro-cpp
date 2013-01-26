package sf.net.experimaestro.server;

import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory;
import org.mortbay.io.Buffer;
import org.mortbay.jetty.AbstractConnector;

import java.io.IOException;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 25/1/13
 */
public class UnixSocketConnector extends AbstractConnector {
    private final JNAUSocketFactory factory;

    public UnixSocketConnector() throws AgentProxyException {
        factory = new JNAUSocketFactory();
    }

    @Override
    protected void accept(int acceptorID) throws IOException, InterruptedException {

        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected Buffer newBuffer(int size) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void open() throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close() throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public Object getConnection() {
        try {
            return factory.open("/tmp/xpm.sock");
        } catch (IOException e) {
            throw new RuntimeException("Could not open socket");
        }
    }
}
