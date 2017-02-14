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

import com.heliosapm.jvmti.agent.Agent;

/**
 * <p>Title: InstancesExample</p>
 * <p>Description: Example acquiring instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.InstancesExample</code></p>
 */

public class InstancesExample {

	/**
	 * Example acquiring instances
	 * @param args none. 
	 */
	public static void main(String[] args) {
		final Agent agent = Agent.getInstance();
		System.gc();
		String[] strings = agent.getInstancesOf(String.class, 20);
		for(int i = 10; i < 15; i++) {
			log("String #%s: [%s]", i, strings[i]);
		}
		strings = null;  // Don't prevent gc of these objects !

		CharSequence[] charSeqs = agent.getInstancesOfAny(CharSequence.class, 20);
		for(int i = 10; i < 15; i++) {
			log("CharSequence#%s: Type:%s, Value:[%s]", i, charSeqs[i].getClass().getName(), charSeqs[i].toString());
		}

		
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
