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
import java.util.regex.Pattern;

/**
 * <p>Title: AgentOptionProcessor</p>
 * <p>Description: Defines an operation performed on a command line or agent option</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.install.AgentOptionProcessor</code></p>
 */

public interface AgentOptionProcessor {
	public static final Pattern NAME_VALUE_SPLITTER = Pattern.compile("=");
	public void commandLine(final String value, final StringBuilder agentOpts, final Map<AgentOption, Object> extracted);
	public void agentOpts(final String value, final Map<AgentOption, Object> extracted);
}
