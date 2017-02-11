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
package com.heliosapm.jvmti.options;

import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.heliosapm.jvmti.BaseTest;
import com.heliosapm.jvmti.install.AgentInstaller;
import com.heliosapm.jvmti.install.AgentOption;

/**
 * <p>Title: TestOptions</p>
 * <p>Description: Tests for command line handling</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.options.TestOptions</code></p>
 */

public class TestCommandLine extends BaseTest {
	
	final String PID = "2837";
	final Properties clProps;
	final String packedProps = "D:sna=fu" + AgentInstaller.DELIM_TERM + "D:foo=bar";
	
	public TestCommandLine() {
		clProps = new Properties();
		clProps.setProperty("foo", "bar");
		clProps.setProperty("sna", "fu");
	}

	@Test
	public void testCommandLinePidOnlyOneArg() {
		testPidOnly(PID, "--pid=" + PID);
	}
	
	@Test
	public void testCommandLinePidOnlyTwoArgs() {
		testPidOnly(PID, "--pid", PID);
	}
	
	@Test
	public void testCommandLinePidOnlyUpperTwoArgs() {
		testPidOnly(PID, "--PID", PID);
	}
	
	@Test
	public void testBadCommandLinePidOnlyTwoArgs() {
		Assert.assertEquals("Empty value for option [PID]", testPidOnly(PID, "--pid=", PID));
	}
	
	@Test
	public void testMissingPid() {
		Assert.assertEquals("Missing mandatory command line option[s]:[PID]", testPidOnly(PID));
	}
	
	@Test
	public void testCommandLinePidTwoArgsPropsOneArg() {
		testPidAndProps(PID, "--d=sna=fu", "--PID", PID, "--d=foo=bar");
	}
	
	@Test
	public void testCommandLinePidTwoArgsPropsTwoArgs() {
		testPidAndProps(PID, "--d", "sna=fu", "--PID", PID, "--d", "foo=bar");
	}
	
	protected String testPidOnly(final String pid, final String...args) {
		final StringBuilder allAgentOptions = new StringBuilder();
		final Map<AgentOption, Object> options;
		try {
			options = AgentOption.commandLine(allAgentOptions, args);
		} catch (Exception ex) {
			return ex.getMessage();
		}
		Assert.assertEquals(pid, options.get(AgentOption.PID));
		Assert.assertEquals(1, options.size());
		Assert.assertEquals(0, allAgentOptions.length());
		return null;
	}
	
	protected String testPidAndProps(final String pid, final String...args) {
		final StringBuilder allAgentOptions = new StringBuilder();
		final Map<AgentOption, Object> options;
		try {
			options = AgentOption.commandLine(allAgentOptions, args);
		} catch (Exception ex) {
			return ex.getMessage();
		}
		Assert.assertEquals(pid, options.get(AgentOption.PID));
		Assert.assertEquals(1, options.size());
		Assert.assertEquals(packedProps, allAgentOptions.toString());
		return null;
	}
	
	
	
	
}
