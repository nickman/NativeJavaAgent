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

import org.junit.Test;
import org.junit.Assert;

import com.heliosapm.jvmti.install.AgentInstaller;
import com.heliosapm.jvmti.install.AgentOption;

/**
 * <p>Title: TestAgentOptions</p>
 * <p>Description: Tests processing of packed agent options</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.options.TestAgentOptions</code></p>
 */

public class TestAgentOptions {
	
	final Properties clProps;
	final String packedProps = "D:sna=fu" + AgentInstaller.DELIM_TERM + "D:foo=bar";
	
	public TestAgentOptions() {
		clProps = new Properties();
		clProps.setProperty("foo", "bar");
		clProps.setProperty("sna", "fu");
	}
	
	@Test
	public void testInstallSysProps() {
		final Map<AgentOption, Object> options = AgentOption.agentOptions(packedProps);
		final Properties p = (Properties)options.get(AgentOption.D);
		Assert.assertNotNull(p);
		Assert.assertEquals(2, p.size());
		Assert.assertEquals("fu", p.getProperty("sna"));
		Assert.assertEquals("bar", p.getProperty("foo"));
	}
	

}
