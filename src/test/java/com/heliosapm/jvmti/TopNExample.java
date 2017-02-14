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
package com.heliosapm.jvmti;

import java.util.Map;

import com.heliosapm.jvmti.agent.Agent;

/**
 * <p>Title: TopNExample</p>
 * <p>Description: Example for top n</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.TopNExample</code></p>
 */

public class TopNExample {

	/**
	 * Lists the top n 
	 * @param args None
	 */
	public static void main(String[] args) {
		final Agent agent = Agent.getInstance();
		System.gc();
		final Map<Class<Object>, Long> topMap = agent.getTopNInstanceCounts(Object.class, 10, false);
		for(Map.Entry<Class<Object>, Long> entry: topMap.entrySet()) {
			log("%s  :  %s", Agent.renderClassName(entry.getKey()), entry.getValue());
		}
		topMap.clear();  // don't prevent gc !
	}
	
	
	/**
	 * Low maintenance formatted message logger
	 * @param fmt The format
	 * @param args The format args
	 */
	public static void log(final Object fmt, final Object...args) {
		System.out.println(String.format(fmt.toString(), args));
	}
	

}
