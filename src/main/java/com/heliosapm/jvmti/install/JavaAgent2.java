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
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.ObjectName;
import javax.management.loading.PrivateMLet;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Logger;
import org.w3c.dom.Node;

import com.heliosapm.jvmti.extension.ExecutionScheduler;
import com.heliosapm.utils.collections.Props;
import com.heliosapm.utils.concurrency.ExtendedThreadManager;
import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.lang.StringHelper;
import com.heliosapm.utils.url.URLHelper;
import com.heliosapm.utils.xml.XMLHelper;

/**
 * <p>Title: JavaAgent2</p>
 * <p>Description: The boot java agent implementation to install the agent</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.install.JavaAgent2</code></p>
 */

public class JavaAgent2 {
	/** The installation supplied instrumentation instance */
	public static Instrumentation INSTRUMENTATION = null;
	
	/** System property overriding the default MLET MBean  */
	public static final String MLET_OBJECT_NAME_PROP = "com.heliosapm.jvmti.classloader.objectname";
	/** The default MLET MBean  */
	public static final String MLET_OBJECT_NAME = "com.heliosapm.jvmti.agent:service=ClassLoader";
	/** The class name of the native agent */
	public static final String AGENT_CLASS_NAME = "com.heliosapm.jvmti.agent.Agent";
	
	/** The agent arg (location of xml config) */
	private static String xmlConfig = "defaultconfig.xml";
	/** The xml config URL */
	private static URL xmlConfigUrl = null;
	/** The xml config XML node */
	private static Node rootConfigNode = null;
	
	
	/**
	 * The agent bootstrap entry point
	 * @param agentArgs The agent initialization arguments
	 * @param inst The instrumentation instance
	 */	
	public static void main(final String agentArgs, final Instrumentation inst) {
		System.err.println("DEBUG: Booting NativeAgent...."); 
		INSTRUMENTATION = inst;
		final Map<AgentOption, Object> agentOptions = AgentOption.agentOptions(agentArgs);
		final Properties p = (Properties)agentOptions.get(AgentOption.D);
		if(p!=null && !p.isEmpty()) installProperties(p);
		@SuppressWarnings("unchecked")
		final Set<URL> classPath = (Set<URL>)agentOptions.get(AgentOption.CP);
		final ClassLoader classLoader = getClassLoader(classPath);
		final ClassLoader current = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			System.err.println("DEBUG: Finding class [" + AGENT_CLASS_NAME + "]");
			final Class<?> agentClass = Class.forName(AGENT_CLASS_NAME, true, JavaAgent2.class.getClassLoader());
			System.err.println("DEBUG: Found class [" + agentClass.getName() + "]");
			final Method method = agentClass.getDeclaredMethod("getInstance");
			System.err.println("DEBUG: Found method [" + method.toGenericString() + "]\n\tInvoking.....");
			// need to delay the invocation in a seperate thread because
			// the Agent will need to attach to itself to load the native agent.
			final Thread agentLoader = new Thread("NativeAgentLoader") {
				public void run() {
					try {
						Thread.currentThread().join(500);  // TODO: make this configurable
						method.invoke(null);
						System.err.println("OK: Native Agent Installed. Configuring...");
						xmlConfigUrl = URLHelper.toURL(xmlConfig);
						Logger.info("XML Config: [{}]", xmlConfigUrl);
						rootConfigNode = XMLHelper.parseXML(xmlConfigUrl).getDocumentElement();
						Logger.debug("First Child Node: [{}]", XMLHelper.renderNode(rootConfigNode));		
						configure();
					} catch (Throwable ex) {
						System.err.println("Failed to load native agent. Stack trace follows:");
						ex.printStackTrace(System.err);
					}					
				}
			};
			agentLoader.setDaemon(true);
			agentLoader.start();
			System.err.println("DEBUG: We're done here");
		} catch (Exception ex) {
			System.err.println("ERROR: Failed to install Agent:" + ex);
		} finally {
			Thread.currentThread().setContextClassLoader(current);
		}
		
	}
	
	protected static void configure() throws Exception {
		if(XMLHelper.hasChildNodeByName(rootConfigNode, "logging")) {
			sysPropsConfig();
			externalLoggingConfig();
			extensionsConfig();
			jmxmpConfig();
			extendedThreadManagerConfig();
		}
	}
	
	private static void extendedThreadManagerConfig() {
		if(XMLHelper.getChildNodeByName(rootConfigNode, "extendedtm") != null) {
			ExtendedThreadManager.install();
		}
	}
	
	private static void sysPropsConfig() {
		Node node = XMLHelper.getChildNodeByName(rootConfigNode, "sysprops");
		if(node!=null) {
			Properties p = Props.strToProps(
					StringHelper.resolveTokens(
							XMLHelper.getNodeTextValue(node)
					)
			);			
			Props.setSystem(p);
		}
	}
	
	private static void externalLoggingConfig() {
		Node node = XMLHelper.getChildNodeByName(rootConfigNode, "logging");
		String externalConfig = XMLHelper.getAttributeByName(node, "config", null);
		if(externalConfig != null) {
			externalConfig = StringHelper.resolveTokens(externalConfig);
		}
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
	
	private static void extensionsConfig() {
		Node node = XMLHelper.getChildNodeByName(rootConfigNode, "extensions");
		for(Node xnode : XMLHelper.getChildNodesByName(node, "extension", false)) {
			String className = XMLHelper.getNodeTextValue(xnode);
			ExecutionScheduler.getInstance().schedule(className);
		}
	}
	
	private static void jmxmpConfig() {
		Node node = XMLHelper.getChildNodeByName(rootConfigNode, "jmxmp");
		if(node!=null) {
			Node portNode = XMLHelper.getChildNodeByName(node, "port");
			if(portNode!=null) {
				Node ifaceNode = XMLHelper.getChildNodeByName(node, "iface");
				String iface = ifaceNode==null ? "127.0.0.1" : StringHelper.resolveTokens(XMLHelper.getNodeTextValue(ifaceNode));
				int port = Integer.parseInt(StringHelper.resolveTokens(XMLHelper.getNodeTextValue(portNode)));
				JMXHelper.fireUpJMXMPServer(iface, port);
			}
		}
	}
	
	
	protected static ClassLoader getClassLoader(final Set<URL> classPath) {
		if(classPath==null) return JavaAgent2.class.getClassLoader();
		final PrivateMLet classLoader = new PrivateMLet(classPath.toArray(new URL[classPath.size()]), true);
		try {
			final ObjectName on = new ObjectName(System.getProperty(MLET_OBJECT_NAME_PROP, MLET_OBJECT_NAME));
			ManagementFactory.getPlatformMBeanServer().registerMBean(classLoader, on);
		} catch (Exception ex) {
			System.err.println("WARNING: Failed to register PrivateMLet MBean:" + ex);
		}
		return classLoader;
	}
	
	/**
	 * Installs agent related system properties
	 * @param p The properties to install
	 */
	protected static void installProperties(final Properties p) {
		for(final String key: p.stringPropertyNames()) {
			System.setProperty(key, p.getProperty(key));
		}
	}

	/**
	 * The agent bootstrap entry point
	 * @param agentArgs The agent initialization arguments
	 * @param inst The instrumentation instance
	 */	
	public static void agentmain(final String agentArgs, final Instrumentation inst) {
		main(agentArgs, inst);
	}
	
	/**
	 * The agent bootstrap entry point
	 * @param agentArgs The agent initialization arguments
	 * @param inst The instrumentation instance
	 */
	public static void premain(final String agentArgs, final Instrumentation inst) {	
		main(agentArgs, inst);
	}
	
	/**
	 * The agent bootstrap entry point which fails the install since there is no instrumentation
	 * @param agentArgs The agent initialization arguments
	 */	
	public static void agentmain(final String agentArgs) {
		main(agentArgs, null);
	}
	
	/**
	 * The agent bootstrap entry point which fails the install since there is no instrumentation
	 * @param agentArgs The agent initialization arguments
	 */	
	public static void premain(final String agentArgs) {
		main(agentArgs, null);
	}
	
}
