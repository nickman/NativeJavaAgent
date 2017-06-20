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
import java.net.URL;

import org.pmw.tinylog.Logger;
import org.w3c.dom.Node;

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

	
	/**
	 * The agent bootstrap entry point
	 * @param agentArgs The agent initialization arguments
	 * @param inst The instrumentation instance
	 */	
	public static void main(final String agentArgs, final Instrumentation inst) {
		INSTRUMENTATION = inst;
		Logger.info("Instrumentation:{}", INSTRUMENTATION!=null);
		if(agentArgs!=null) {
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
		rootConfigNode = XMLHelper.parseXML(xmlConfigUrl);
		configure();
	}
	
	private static void configure() {
		externalLoggingConfig();
	}
	
	private static void externalLoggingConfig() {
		
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
