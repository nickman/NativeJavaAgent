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
package com.heliosapm.jvmti.agent;

import java.util.LinkedHashMap;
import java.util.LongSummaryStatistics;

/**
 * <p>Title: AgentMBean</p>
 * <p>Description: JMX MBean interface for {@link Agent}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.agent.AgentMBean</code></p>
 */

public interface AgentMBean {
	/** The JMX ObjectName */
	public static final String OBJECT_NAME = "com.heliosapm.jvmti:service=Agent";
	
	/**
	 * Returns the number of instances of the passed class 
	 * or any that implement or inherrit from it 
	 * @param className The name of the class to count instances for
	 * @return the number of instances found
	 */
	public int getInstanceCountOfAny(final String className);

	/**
	 * Returns the number of instances of the exact passed class 
	 * @param className The name of the class to count instances for
	 * @return the number of instances found
	 */
	public int getInstanceCountOf(final String className);
	
	
	/**
	 * Returns the top <code>N</code> classes by count
	 * @param className The name of the class to count instances for
	 * @param n The top n value
	 * @param excludePrims exclude primitives and arrays of primitives
	 * @return A map of the number of class instances keyed by the class name
	 */
	public LinkedHashMap<String, Long> getTopNInstanceCounts(final String className, final int n, final boolean excludePrims);
	
	
	/**
	 * Indicates if the agent was loaded at boot time or was attached
	 * @return true if loaded, false if attached
	 */
	public boolean isAgentBootLoaded();
	
	/**
	 * Returns the location where the native library was loaded from
	 * @return the library file name 
	 */
	public String getNativeLibrary();
	
	/**
	 * Returns the total number of completed topN operations
	 * @return the total number of completed topN operations
	 * @see com.heliosapm.jvmti.util.TimerHistory#count()
	 */
	public long getTopNCount();

	/**
	 * Returns the average elapsed time of recent completed topN operations in ms.
	 * @return the average elapsed time of recent completed topN operations
	 * @see com.heliosapm.jvmti.util.TimerHistory#average()
	 */
	public double getTopNAverage();

	/**
	 * Returns the maximum elapsed time of recent completed topN operations in ms.
	 * @return the maximum elapsed time of recent completed topN operations
	 * @see com.heliosapm.jvmti.util.TimerHistory#max()
	 */
	public long getTopNMax();

	/**
	 * Returns the minimum elapsed time of recent completed topN operations in ms.
	 * @return the minimum elapsed time of recent completed topN operations
	 * @see com.heliosapm.jvmti.util.TimerHistory#min()
	 */
	public long getTopNMin();
	
	/**
	 * Returns the elapsed time pf the most recent topN operation in ms.
	 * @return the elapsed time pf the most recent topN operation
	 * @see com.heliosapm.jvmti.util.TimerHistory#min()
	 */
	public long getTopNLast();
	
	/**
	 * Returns combined statistics for recent topN operations 
	 * @return combined statistics for recent topN operations 
	 */
	public LongSummaryStatistics getTopNStats();
	
	/**
	 * Resets the timers but not the counters
	 */
	public void resetTimers();
	
	/**
	 * Resets the timers and the counters
	 */
	public void resetTimersAll();
	
}
