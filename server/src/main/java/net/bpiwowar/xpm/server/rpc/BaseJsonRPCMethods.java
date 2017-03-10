package net.bpiwowar.xpm.server.rpc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServlet;

/**
 *
 */
public class BaseJsonRPCMethods extends HttpServlet implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getFormatterLogger();
    protected final JSONRPCRequest mos;

    public BaseJsonRPCMethods(JSONRPCRequest mos) {
        this.mos = mos;
    }


    @Override
    public void close() {

    }
}
