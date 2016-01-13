package net.bpiwowar.xpm.connectors;

import org.w3c.dom.Document;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;

/**
 *
 */
public class OARStat {
    private final Document document;

    public boolean isRunning() {
        String state = OARLauncher.evaluateXPathToString("//item[@key = \"state\"]/text()", document);
        return "running".equalsIgnoreCase(state);
    }

    public int exitValue() {
        String state = OARLauncher.evaluateXPathToString("//item[@key = \"state\"]/text()", document);
        if ("running".equalsIgnoreCase(state))
            throw new IllegalThreadStateException("Job is running - cannot access its exit value");

        String code = OARLauncher.evaluateXPathToString("//item[@key = \"exit_code\"]/text()", document);

        if ("".equals(code))
            return -1;
        return Integer.parseInt(code);
    }

    /**
     * Runs oarstat and returns the XML document
     */
    OARStat(SingleHostConnector connector, String pid, boolean full) {
        try {
            if (full) {
                document = OARLauncher.exec(connector, "oarstat", "--xml", "--full", "--job", pid);
            } else {
                document = OARLauncher.exec(connector, "oarstat", "--xml", "--job", pid);
            }
        } catch (Exception e) {
            throw new XPMRuntimeException(e, "Cannot parse oarstat output");
        }
    }

}
