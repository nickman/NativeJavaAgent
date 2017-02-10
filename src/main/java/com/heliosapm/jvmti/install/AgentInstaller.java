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

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

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
	
	
	/**
	 * Entry point to invoke the agent installer
	 * @param args The installer directives: <ul>
	 * 	<li><b>--pid &lt;jvm id&gt;</b> The target JVM to install into</li>
	 *  <li><b>--D &lt;key&gt;=&lt;value&gt;</b> Specifies a system property to set before launching the agent.
	 *  Can be specified multiple times.</li>
	 * </ul>
	 */
	public static void main(final String[] args) {
		
	}
	
	/**
	 * The agent bootstrap entry point
	 * @param agentArgs The agent initialization arguments
	 * @param inst The instrumentation instance
	 */	
	public static void main(final String agentArgs, final Instrumentation inst) {
	}
	
	private static String[] args(final String delim, final String agentArgs) {
		final List<String> l = new ArrayList<String>();
		final StringTokenizer tokenizer = new StringTokenizer(agentArgs, delim);
		while (tokenizer.hasMoreTokens()) {
			final String token = tokenizer.nextToken();
			if(token!=null && !token.trim().isEmpty()) {
				l.add(token.trim());
			}
		}
		return l.toArray(new String[0]);
	}
	
	private static String delim(final String agentArgs) {
		final int index = agentArgs.indexOf(DELIM_TERM);
		if(index<1) throw new IllegalArgumentException("The agent arguments [" + agentArgs + "] had no delimeter termination.");
		return agentArgs.substring(0, index);
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
