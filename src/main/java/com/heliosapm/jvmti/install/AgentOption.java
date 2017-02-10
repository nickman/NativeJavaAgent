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
import java.util.Set;

import com.heliosapm.shorthand.attach.vm.VirtualMachine;

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
			extracted.put(this, VirtualMachine.attach(value));
		}
	},
	/** A system property to set in the install target */
	D(true, true, false, true, false){
		@Override
		public void agentOpts(final String value, final Map<AgentOption, Object> extracted) {
			final String[] sysProp = NAME_VALUE_SPLITTER.split(value);
			if(sysProp.length==2) {
				if(sysProp[0]==null || sysProp[0].trim().isEmpty() || sysProp[1]==null || sysProp[1].trim().isEmpty()) {
					throw new RuntimeException("Invalid system property definition: [" + value + "]");
				}
				System.setProperty(sysProp[0].trim(), sysProp[1].trim());
			} else {
				throw new RuntimeException("Invalid system property definition: [" + value + "]");
			}
		}
		@Override
		public void commandLine(final String value, final StringBuilder agentOpts, final Map<AgentOption, Object> extracted) {			
			if(agentOpts.length()!=0) {
				agentOpts.append("##");
			}
			agentOpts.append(name()).append(":").append(value);			
		}
	};
	
	private AgentOption(final boolean dupsAllowed, final boolean passedToTarget, final boolean flag, final boolean madatory, final boolean madatoryOpt) {
		this.dupsAllowed = dupsAllowed;
		this.passedToTarget = passedToTarget;
		this.flag = flag;
		this.madatoryCl = madatory;
		this.madatoryOpt = madatoryOpt;
	}
	
	/** Indicates if this option is allowed to be supplied more than once */
	public final boolean dupsAllowed;
	/** Indicates if this command line option is relayed to the target JVM as an agent option */
	public final boolean passedToTarget;
	/** Indicates if this command line option is a flag with no associated value */
	public final boolean flag;
	/** Indicates if this command line option is mandatory */
	public final boolean madatoryCl;
	/** Indicates if this agent option is mandatory */
	public final boolean madatoryOpt;
	
	private static final AgentOption[] values = values();
	public static final Set<AgentOption> mandatoryCommandLine;
	public static final Set<AgentOption> mandatoryOption;
	
	static {
		final Set<AgentOption> tmpCl = EnumSet.noneOf(AgentOption.class);
		final Set<AgentOption> tmpOpt = EnumSet.noneOf(AgentOption.class);
		for(AgentOption ao: values) {
			if(ao.madatoryCl) tmpCl.add(ao);
		}
		mandatoryCommandLine = Collections.unmodifiableSet(tmpCl);
		mandatoryOption = Collections.unmodifiableSet(tmpOpt);
	}
	
	private static Set<AgentOption> mandatoryCl() {
		return EnumSet.copyOf(mandatoryCommandLine);
	}
	
	private static Set<AgentOption> mandatoryOpt() {
		return EnumSet.copyOf(mandatoryOption);
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
	
	
	public static Map<AgentOption, Object> commandLine(final String[] commandLine, final StringBuilder allAgentOptions) {
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
				if(index!=-1) {
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
					agentOption = v.substring(0, index);
					ao = decode(agentOption);
					optionValue = v.substring(index+1);
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
