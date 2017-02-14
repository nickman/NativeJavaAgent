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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.heliosapm.shorthand.attach.vm.VirtualMachine;

//import com.heliosapm.utils.classload.IsolatedClassLoader;

/**
 * <p>Title: AgentInstaller</p>
 * <p>Description: Installs the OIF agent in the target JVM</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.install.AgentInstaller</code></p>
 */

public class AgentInstaller {
	/** Static class logger */
	private final static Logger LOG = Logger.getLogger(AgentInstaller.class.getName());
	
	/** The args delimeter terminator */
	public static final String DELIM_TERM = "##";
	/** The booted agent instance */
	public static Object bootedAgent = null;
	
	/** System property set in target JVM when agent is installed */
	public static final String AGENT_INSTALLED_PROP = "com.heliosapm.jvmti.agent.installed";
	
	/**
	 * Entry point to invoke the agent installer
	 * @param args The installer directives: <ul>
	 * 	<li><b>--pid &lt;jvm id&gt;</b> The target JVM to install into</li>
	 *  <li><b>--D &lt;key&gt;=&lt;value&gt;</b> Specifies a system property to set before launching the agent.
	 *  Can be specified multiple times.</li>
	 * </ul>
	 */
	public static void main(final String[] args) {
		final StringBuilder packedAgentOptions = new StringBuilder();
		final Map<AgentOption, Object> agentOptions = AgentOption.commandLine(packedAgentOptions, args);
		install(agentOptions, packedAgentOptions.toString());
	}
	
	private static void install(final Map<AgentOption, Object> agentOptions, final String packedAgentOptions) {		
		VirtualMachine vm = null;
		try {
			final String pid = (String)agentOptions.get(AgentOption.PID);
			LOG.log(Level.INFO, "Installing Agent into JVM [" + pid + "]...");
			vm = VirtualMachine.attach(pid);
			if(vm.getSystemProperties().containsKey(AGENT_INSTALLED_PROP)) {
				LOG.log(Level.WARNING, "Agent already installed in JVM [" + pid + "]");
				return;
			}
			final String jarFile = AgentInstaller.class.getProtectionDomain().getCodeSource().getLocation().getFile();
			LOG.log(Level.INFO, "Agent jar [" + jarFile + "]");
			if(packedAgentOptions.isEmpty()) {
				LOG.log(Level.INFO, "Executing [vm.loadAgent(\"" + jarFile + "\")]");
				vm.loadAgent(jarFile);				
			} else {
				LOG.log(Level.INFO, "Executing [vm.loadAgent(\"" + jarFile + "\",\"" + packedAgentOptions + "\")]");
				vm.loadAgent(jarFile, packedAgentOptions);
			}
			LOG.log(Level.INFO, "Successfully installed Agent jar [" + jarFile + "] into JVM [" + pid + "]");
		} catch (Throwable ex) {
			LOG.log(Level.SEVERE, "Failed to install Agent", ex);
		} finally {
			if(vm!=null) try { vm.detach(); } catch (Exception x) {/* No Op */}
		}
	}

}
