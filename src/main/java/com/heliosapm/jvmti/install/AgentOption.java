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

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>Title: AgentOption</p>
 * <p>Description: Enumeration of agent options</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.install.AgentOption</code></p>
 */

public enum AgentOption implements AgentOptionProcessor {
	/** The process ID of the JVM to install to */
	PID(false, false, false, true, false){
		@Override
		public void agentOpts(final String value, final Map<AgentOption, Object> extracted) {
			/* No Op */
		}
		@Override
		public void commandLine(final String value, final StringBuilder agentOpts, final Map<AgentOption, Object> extracted) {			
			extracted.put(this, value);
		}
	},
	/** A system property to set in the install target */
	D(true, true, false, false, false){
		@Override
		public void agentOpts(final String value, final Map<AgentOption, Object> extracted) {
			final String[] sysProp = NAME_VALUE_SPLITTER.split(value);
			if(sysProp.length==2) {
				if(sysProp[0]==null || sysProp[0].trim().isEmpty() || sysProp[1]==null || sysProp[1].trim().isEmpty()) {
					throw new RuntimeException("Invalid system property definition: [" + value + "]");
				}
				Properties p = (Properties) extracted.get(this);
				if(p==null) {
					p = new Properties();
					extracted.put(this, p);
				}
				p.setProperty(sysProp[0].trim(), sysProp[1].trim());
			} else {
				throw new RuntimeException("Invalid system property definition: [" + value + "]");
			}
		}
		@Override
		public void commandLine(final String value, final StringBuilder agentOpts, final Map<AgentOption, Object> extracted) {			
			if(agentOpts.length()!=0) {
				agentOpts.append(AgentInstaller.DELIM_TERM);
			}
			agentOpts.append(name()).append(":").append(value);			
		}
	};
	
	private AgentOption(final boolean dupsAllowed, final boolean passedToTarget, final boolean flag, final boolean mandatoryCl, final boolean mandatoryOpt) {
		this.dupsAllowed = dupsAllowed;
		this.passedToTarget = passedToTarget;
		this.flag = flag;
		this.mandatoryCl = mandatoryCl;
		this.mandatoryOpt = mandatoryOpt;
	}
	
	/** Indicates if this option is allowed to be supplied more than once */
	public final boolean dupsAllowed;
	/** Indicates if this command line option is relayed to the target JVM as an agent option */
	public final boolean passedToTarget;
	/** Indicates if this command line option is a flag with no associated value */
	public final boolean flag;
	/** Indicates if this command line option is mandatory */
	public final boolean mandatoryCl;
	/** Indicates if this agent option is mandatory */
	public final boolean mandatoryOpt;
	
	private static final AgentOption[] values = values();
	
	/** The packed agent options splitter */
	public static final Pattern PACKED_SPLITTER = Pattern.compile(AgentInstaller.DELIM_TERM);
	/** Options that are mandatory on the command line */	
	public static final Set<AgentOption> mandatoryCommandLine;
	/** Options that are mandatory in the agent options */
	public static final Set<AgentOption> mandatoryOption;
	
	static {
		final Set<AgentOption> tmpCl = EnumSet.noneOf(AgentOption.class);
		final Set<AgentOption> tmpOpt = EnumSet.noneOf(AgentOption.class);
		for(AgentOption ao: values) {
			if(ao.mandatoryCl) tmpCl.add(ao);
		}
		mandatoryCommandLine = Collections.unmodifiableSet(tmpCl);
		mandatoryOption = Collections.unmodifiableSet(tmpOpt);
	}
	
	private static Set<AgentOption> mandatoryCl() {
		return EnumSet.copyOf(mandatoryCommandLine);
	}
	
	private static Set<AgentOption> mandatoryOpt() {
		// we have none for now
		//return EnumSet.copyOf(mandatoryOption);
		return EnumSet.noneOf(AgentOption.class);
	}
	
	
	/**
	 * Decodes the passed string to an agent option
	 * @param code The code to decode
	 * @return The decoded AgentOption
	 */
	public static AgentOption decode(final String code) {
		if(code==null || code.trim().isEmpty()) throw new IllegalArgumentException("The passed code was null");
		final String _code = code.trim().toUpperCase();
		try {
			return valueOf(_code);
		} catch (Exception ex) {
			throw new IllegalArgumentException("Invalid AgentOption [" + code + "]");
		}
	}
	
	/**
	 * Handles agent option processing
	 * @param packedAgentOptions The string passed to the agent containing the options to appy
	 * @return A map of actionable options extracted from the packed options
	 */
	public static Map<AgentOption, Object> agentOptions(final String packedAgentOptions) {
		final Set<AgentOption> mandatory = mandatoryOpt();
		final Map<AgentOption, Object> map = new EnumMap<AgentOption, Object>(AgentOption.class);
		if(packedAgentOptions!=null && !packedAgentOptions.trim().isEmpty()) {
			final String[] options = PACKED_SPLITTER.split(packedAgentOptions);
			for(String option: options) {
				final int index = option.indexOf(':');
				if(index==-1) throw new IllegalArgumentException("Invalid unpacked agent option [" + option + "]");
				final String agentOption = option.substring(0, index);
				final AgentOption ao = decode(agentOption);
				final String optionValue;
				if(ao.flag) {
					optionValue = null;
				} else {
					optionValue = option.substring(index+1);
					if(optionValue.isEmpty()) throw new IllegalArgumentException("Empty unpacked agent option value [" + option + "]");
				}
				ao.agentOpts(optionValue, map);
				mandatory.remove(ao);
			}
		}
		if(!mandatory.isEmpty()) {
			throw new RuntimeException("Missing mandatory agent option[s]:" + mandatory);
		}
		
		return map;
	}
	
	/**
	 * Handles command line processing
	 * @param allAgentOptions A buffer that collects all command line options intended to
	 * be relayed to a jmv installation as agent options
	 * @param commandLine The command line options to parse
	 * @return A map of actionable options extracted from the command line
	 */
	public static Map<AgentOption, Object> commandLine(final StringBuilder allAgentOptions, final String...commandLine) {
		final Set<AgentOption> mandatory = mandatoryCl();
		final int maxIndex = commandLine.length-1;
		final Map<AgentOption, Object> map = new EnumMap<AgentOption, Object>(AgentOption.class);
		for(int i = 0; i < commandLine.length; i++) {
			if(commandLine[i].indexOf("--")==0) {
				final String agentOption;
				final String optionValue;
				final AgentOption ao;
				String v = commandLine[i].substring(2);
				final int index = v.indexOf('=');
				if(index==-1) {
					//=============================================
					// option is flag, or value is next arg
					//=============================================
					agentOption = v;
					ao = decode(agentOption);
					if(!ao.flag) {
						i++;
						if(i > maxIndex) {
							throw new RuntimeException("No value provided for option [" + ao.name() + "]");
						}
						optionValue = commandLine[i];						
					} else {
						optionValue = null;
					}
				} else {
					//=============================================
					// option is in form OPT=VAL
					//=============================================					
					agentOption = v.substring(0, index);
					ao = decode(agentOption);
					optionValue = v.substring(index+1);
					if(optionValue.isEmpty()) {
						throw new RuntimeException("Empty value for option [" + ao.name() + "]");
					}					
				}
				ao.commandLine(optionValue, allAgentOptions, map);
				mandatory.remove(ao);
				
			}
		}
		if(!mandatory.isEmpty()) {
			throw new RuntimeException("Missing mandatory command line option[s]:" + mandatory);
		}
		return map;
	}
}
