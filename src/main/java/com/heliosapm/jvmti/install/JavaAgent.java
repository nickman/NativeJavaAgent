// This file is part of OpenTSDB.
// Copyright (C) 2010-2016  The OpenTSDB Authors.
//
// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 2.1 of the License, or (at your
// option) any later version.  This program is distributed in the hope that it
// will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
// General Public License for more details.  You should have received a copy
// of the GNU Lesser General Public License along with this program.  If not,
// see <http://www.gnu.org/licenses/>.
package com.heliosapm.jvmti.install;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URL;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Logger;
import org.w3c.dom.Node;

import com.heliosapm.jvmti.agent.NativeAgent;
import com.heliosapm.utils.url.URLHelper;
import com.heliosapm.utils.xml.XMLHelper;

/**
 * <p>Title: JavaAgent</p>
 * <p>Description: The bootstrap java agent</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.install.JavaAgent</code></p>
 */

public class JavaAgent {
	/** The installation supplied instrumentation instance */
	public static Instrumentation INSTRUMENTATION = null;
	/** The agent arg (location of xml config) */
	private static String xmlConfig = "defaultconfig.xml";
	/** The xml config URL */
	private static URL xmlConfigUrl = null;
	/** The xml config XML node */
	private static Node rootConfigNode = null;

	public static void main(String[] args) {
		main("", (Instrumentation)null);
	}
	
	/**
	 * The agent bootstrap entry point
	 * @param agentArgs The agent initialization arguments
	 * @param inst The instrumentation instance
	 */	
	public static void main(final String agentArgs, final Instrumentation inst) {
		INSTRUMENTATION = inst;
		Logger.info("Instrumentation:{}", INSTRUMENTATION!=null);
		if(agentArgs!=null && !agentArgs.trim().isEmpty()) {
			Logger.info("Supplied Agent Args: [{}]", agentArgs);			
			URL url = URLHelper.toURL(agentArgs); 
			if(URLHelper.resolves(url)) {
				xmlConfigUrl = url;
			} else {
				Logger.warn("Supplied XML Config Could Not Be Resolved: [{}]", agentArgs);
			}
		}
		if(xmlConfigUrl==null) {
			xmlConfigUrl = URLHelper.toURL(xmlConfig);
		}
		Logger.info("XML Config: [{}]", xmlConfigUrl);
		rootConfigNode = XMLHelper.parseXML(xmlConfigUrl).getDocumentElement();
		Logger.debug("First Child Node: [{}]", XMLHelper.renderNode(rootConfigNode));		
		configure();
	}
	
	private static void configure() {
		if(XMLHelper.hasChildNodeByName(rootConfigNode, "logging")) {
			externalLoggingConfig();
		}
		loadNative();
	}
	
	private static void externalLoggingConfig() {
		Node node = XMLHelper.getChildNodeByName(rootConfigNode, "logging");
		String externalConfig = XMLHelper.getAttributeByName(node, "config", null);
		if(URLHelper.resolves(URLHelper.toURL(externalConfig))) {
			try {
				URL configUrl = URLHelper.toURL(externalConfig);
				Configurator.fromURL(configUrl);
				Logger.info("Configured logging from external url: [{}]", configUrl);
			} catch (IOException iex) {
				Logger.warn("Failed to configure logging from external: [{}]", externalConfig, iex);
			}
		}
	}
	
	private static void loadNative() {		
		Node node = XMLHelper.getChildNodeByName(rootConfigNode, "native");
		if(node!=null && XMLHelper.getAttributeByName(node, "disabled", false)) {
			Logger.info("Native Agent Disabled");
			return;
		}
		NativeAgent.getInstance();
	}

	/**
	 * The agent bootstrap entry point
	 * @param agentArgs The agent initialization arguments
	 * @param inst The instrumentation instance
	 */	
	public static void agentmain(final String agentArgs, final Instrumentation inst) {
		Logger.info("agentmain:2");
		main(agentArgs, inst);
	}
	
	/**
	 * The agent bootstrap entry point
	 * @param agentArgs The agent initialization arguments
	 * @param inst The instrumentation instance
	 */
	public static void premain(final String agentArgs, final Instrumentation inst) {
		Logger.info("premain:2");
		main(agentArgs, inst);
	}
	
	/**
	 * The agent bootstrap entry point which fails the install since there is no instrumentation
	 * @param agentArgs The agent initialization arguments
	 */	
	public static void agentmain(final String agentArgs) {
		Logger.info("agentmain:1");
		main(agentArgs, null);
	}
	
	/**
	 * The agent bootstrap entry point which fails the install since there is no instrumentation
	 * @param agentArgs The agent initialization arguments
	 */	
	public static void premain(final String agentArgs) {
		Logger.info("premain:1");
		main(agentArgs, null);
	}
	
}
