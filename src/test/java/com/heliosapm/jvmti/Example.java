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
 * <p>Title: Example</p>
 * <p>Description: Simple examples of the agent</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.Example</code></p>
 */

public class Example {

	/**
	 * Some basic examples
	 * @param args none.
	 */
	public static void main(String[] args) {
		
		final Agent agent = Agent.getInstance();
		System.gc();
		final long startTime = System.currentTimeMillis();
		
		final int objectCount = agent.getInstanceCountOf(Object.class);
		log("Object instance count:%s", objectCount);
		
		final int charSequenceCount = agent.getInstanceCountOfAny(CharSequence.class);
		log("CharSequence instance count:%s", charSequenceCount);
		
		log("Elapsed:%s ms.", System.currentTimeMillis()-startTime);
		
		int total = 0;
		for(int i = 0; i < 100; i++) {
			final int oc = agent.getInstanceCountOf(Object.class);
			final int cc = agent.getInstanceCountOfAny(CharSequence.class);
			total += (oc + cc);
			System.gc();
		}

		final long startTime2 = System.currentTimeMillis();
		
		final int objectCount2 = agent.getInstanceCountOf(Object.class);
		log("Object instance count:%s", objectCount2);
		
		final int charSequenceCount2 = agent.getInstanceCountOfAny(CharSequence.class);
		log("CharSequence instance count:%s", charSequenceCount2);
		
		log("Elapsed:%s ms.", System.currentTimeMillis()-startTime2);
		
		
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
