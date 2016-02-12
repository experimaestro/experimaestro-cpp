//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package net.bpiwowar.xpm.connectors;

import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.ConnectorFactory;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;
import com.pastdev.jsch.SessionFactory;
import com.pastdev.jsch.Slf4jBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class DefaultSessionFactory implements SessionFactory {
    private static Logger logger = LoggerFactory.getLogger(DefaultSessionFactory.class);
    private Map<String, String> config;
    private File dotSshDir;
    private String hostname;
    JSch jsch;
    private int port;
    private Proxy proxy;
    private String username;

    public DefaultSessionFactory(boolean useAgent) {
        this(null, null, null, useAgent);
    }

    public DefaultSessionFactory(String username, String hostname, Integer port, boolean useAgent) {
        this.port = 22;
        JSch.setLogger(new Slf4jBridge());
        this.jsch = new JSch();

        try {
            if (useAgent) {
                this.setDefaultIdentities();
            }
        } catch (JSchException var6) {
            logger.warn("Unable to set default identities: ", var6);
        }

        try {
            this.setDefaultKnownHosts();
        } catch (JSchException var5) {
            logger.warn("Unable to set default known_hosts: ", var5);
        }

        if(username == null) {
            this.username = System.getProperty("user.name").toLowerCase();
        } else {
            this.username = username;
        }

        if(hostname == null) {
            this.hostname = "localhost";
        } else {
            this.hostname = hostname;
        }

        if(port == null) {
            this.port = 22;
        } else {
            this.port = port;
        }

    }

    private DefaultSessionFactory(JSch jsch, String username, String hostname, int port, Proxy proxy) {
        this.port = 22;
        this.jsch = jsch;
        this.username = username;
        this.hostname = hostname;
        this.port = port;
        this.proxy = proxy;
    }

    private void clearIdentityRepository() throws JSchException {
        this.jsch.setIdentityRepository(null);
        this.jsch.removeAllIdentity();
    }

    private File dotSshDir() {
        if(this.dotSshDir == null) {
            String dotSshString = System.getProperty("jsch.dotSsh");
            if(dotSshString != null) {
                this.dotSshDir = new File(dotSshString);
            } else {
                this.dotSshDir = new File(new File(System.getProperty("user.home")), ".ssh");
            }
        }

        return this.dotSshDir;
    }

    public String getHostname() {
        return this.hostname;
    }

    public int getPort() {
        return this.port;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public String getUsername() {
        return this.username;
    }

    public Session newSession() throws JSchException {
        Session session = this.jsch.getSession(this.username, this.hostname, this.port);
        if(this.config != null) {
            Iterator i$ = this.config.keySet().iterator();

            while(i$.hasNext()) {
                String key = (String)i$.next();
                session.setConfig(key, this.config.get(key));
            }
        }

        if(this.proxy != null) {
            session.setProxy(this.proxy);
        }

        return session;
    }

    public SessionFactoryBuilder newSessionFactoryBuilder() {
        return new SessionFactoryBuilder(this.jsch, this.username, this.hostname, this.port, this.proxy, this.config) {
            public SessionFactory build() {
                DefaultSessionFactory sessionFactory = new DefaultSessionFactory(this.jsch, this.username, this.hostname, this.port, this.proxy);
                sessionFactory.config = this.config;
                return sessionFactory;
            }
        };
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public void setConfig(String key, String value) {
        if(this.config == null) {
            this.config = new HashMap();
        }

        this.config.put(key, value);
    }

    private void setDefaultKnownHosts() throws JSchException {
        String knownHosts = System.getProperty("jsch.knownHosts.file");
        if(knownHosts != null && !knownHosts.isEmpty()) {
            this.setKnownHosts(knownHosts);
        } else {
            File knownHostsFile = new File(this.dotSshDir(), "known_hosts");
            if(knownHostsFile.exists()) {
                this.setKnownHosts(knownHostsFile.getAbsolutePath());
            }
        }

    }

    private void setDefaultIdentities() throws JSchException {
        boolean identitiesSet = false;

        try {
            Connector privateKeyFiles = ConnectorFactory.getDefault().createConnector();
            if(privateKeyFiles != null) {
                logger.info("An AgentProxy Connector was found, check for identities");
                RemoteIdentityRepository arr$ = new RemoteIdentityRepository(privateKeyFiles);
                Vector len$ = arr$.getIdentities();
                if(len$.size() > 0) {
                    logger.info("Using AgentProxy identities: {}", len$);
                    this.setIdentityRepository(arr$);
                    identitiesSet = true;
                }
            }
        } catch (AgentProxyException var7) {
            logger.debug("Failed to load any keys from AgentProxy:", var7);
        }

        if(!identitiesSet) {
            String var8 = System.getProperty("jsch.privateKey.files");
            if(var8 != null && !var8.isEmpty()) {
                logger.info("Using local identities from {}: {}", "jsch.privateKey.files", var8);
                this.setIdentitiesFromPrivateKeys(Arrays.asList(var8.split(",")));
                identitiesSet = true;
            }
        }

        if(!identitiesSet) {
            ArrayList var9 = new ArrayList();
            File[] var10 = new File[]{new File(this.dotSshDir(), "id_rsa"), new File(this.dotSshDir(), "id_dsa"), new File(this.dotSshDir(), "id_ecdsa")};
            int var11 = var10.length;

            for (File file : var10) {
                if (file.exists()) {
                    var9.add(file.getAbsolutePath());
                }
            }

            logger.info("Using local identities: {}", var9);
            this.setIdentitiesFromPrivateKeys(var9);
        }

    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setIdentityFromPrivateKey(String privateKey) throws JSchException {
        this.clearIdentityRepository();
        this.jsch.addIdentity(privateKey);
    }

    public void setIdentitiesFromPrivateKeys(List<String> privateKeys) throws JSchException {
        this.clearIdentityRepository();
        Iterator i$ = privateKeys.iterator();

        while(i$.hasNext()) {
            String privateKey = (String)i$.next();
            this.jsch.addIdentity(privateKey);
        }

    }

    public void setIdentityRepository(IdentityRepository identityRepository) {
        this.jsch.setIdentityRepository(identityRepository);
    }

    public void setKnownHosts(InputStream knownHosts) throws JSchException {
        this.jsch.setKnownHosts(knownHosts);
    }

    public void setKnownHosts(String knownHosts) throws JSchException {
        this.jsch.setKnownHosts(knownHosts);
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String toString() {
        return (this.proxy == null?"":this.proxy.toString() + " ") + "ssh://" + this.username + "@" + this.hostname + ":" + this.port;
    }
}
