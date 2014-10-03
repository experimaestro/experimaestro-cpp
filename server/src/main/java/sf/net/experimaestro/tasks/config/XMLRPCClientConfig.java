/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.tasks.config;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.ArgumentPostProcessor;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public class XMLRPCClientConfig {
	final static private Logger LOGGER = Logger.getLogger();

	@Argument(name = "file", help = "The name of the file containing the XML RPC configuration for the client", required=true)
	File xmlrpcfile;

	private Properties xmlrpcConfig;

	
	@ArgumentPostProcessor()
	public void init() throws IOException {
		LOGGER.info("Loading properies from file %s", xmlrpcfile);
		xmlrpcConfig = new Properties();
		xmlrpcConfig.load(new FileInputStream(xmlrpcfile));
	}

    public XMLRPCClientConfig(File xmlrpcfile) throws IOException {
        this.xmlrpcfile = xmlrpcfile;
        init();
    }

    public XMLRPCClientConfig() {
    }

    /**
	 * 
	 * Get a property defined in the file
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public String getProperty(String key, String defaultValue) {
		final String value = xmlrpcConfig.getProperty(key);
		if (value == null)
			return defaultValue;
		return value;
	}

	/**
	 * Gets a client from the RPC file
	 * 
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws MalformedURLException
	 */
	public XmlRpcClient getClient() throws IOException, FileNotFoundException,
			MalformedURLException {

		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		final String url = xmlrpcConfig.getProperty("url");
		LOGGER.info("XML RPC server is at %s", url);
		config.setServerURL(new URL(url));

		if (xmlrpcConfig.contains("user")) {
			config.setBasicUserName(xmlrpcConfig.getProperty("user"));
			config.setBasicPassword(xmlrpcConfig.getProperty("password"));
		}

		LOGGER.info("Connecting to server %s with username %s", url, config.getBasicUserName());
		XmlRpcClient client = new XmlRpcClient();
		client.setConfig(config);
		return client;
	}
}
