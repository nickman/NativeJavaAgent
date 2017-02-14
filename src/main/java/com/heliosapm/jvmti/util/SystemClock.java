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
package com.heliosapm.jvmti.util;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * <p>Title: SystemClock</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.util.SystemClock</code></p>
 */

public class SystemClock {
	
	/**
	 * Starts a new timer
	 * @return the elapsed time object on which elapsed times can be drawn
	 */
	public static ElapsedTime startClock() {
		return new ElapsedTime();
	}
	
	
	/**
	 * Sleeps for the specified number of ms The number of ms to sleep for.
	 * @param ms
	 */
	public static void sleep(final long ms) {
		try { Thread.currentThread().join(ms); } catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Sleeps for the specified period
	 * @param time The time to sleep for
	 * @param unit The unit of time. If null, defaults to {@link TimeUnit#MILLISECONDS}
	 */
	public static void sleep(final long time, final TimeUnit unit) {
		final TimeUnit u = unit==null ? TimeUnit.MILLISECONDS : unit;
		sleep(TimeUnit.MILLISECONDS.convert(time, u));
	}
	
	
	/**
	 * Returns the current time in ms.
	 * @return the current time in ms.
	 */
	public static long time() {
		return System.currentTimeMillis();
	}
	
	/**
	 * Returns the current time in milliseconds to second precision
	 * @return the second precision current timestamp in ms.
	 */
	public static long rtime() {
		return TimeUnit.MILLISECONDS.convert(TimeUnit.SECONDS.convert(time(), TimeUnit.MILLISECONDS), TimeUnit.SECONDS);
	}
	
	/**
	 * Returns a JDBC timestamp for the current time
	 * @return a JDBC timestamp for the current time
	 */
	public static java.sql.Timestamp getTimestamp() {
		return new java.sql.Timestamp(time());
	}
	
	/**
	 * Returns the current time in Unix Time (s.)
	 * @return the current time in Unix Time
	 */
	public static long unixTime() {
		return TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Returns the JVM up time in ms.
	 * @return the up time in ms.
	 */
	public static long upTime() {
		return ManagementFactory.getRuntimeMXBean().getUptime();
	}
	
	/**
	 * Returns the JVM up time in SNMP tick time.
	 * @return the up time in SNMP tick time
	 */
	public static long upTimeTicks() {
		return toTicks(ManagementFactory.getRuntimeMXBean().getUptime());
	}
	

	/**
	 * Converts the passed time to SNMP ticks
	 * @param time The time to convert
	 * @param unit The unit the time is in
	 * @return the time in SNMP ticks
	 */
	public static long toTicks(final long time, final TimeUnit unit) {
		long tt = TimeUnit.MILLISECONDS.convert(time, unit)/100;
		return tt;
	}

	/**
	 * Converts the passed time to SNMP ticks
	 * @param time The time to convert
	 * @return the time in SNMP ticks
	 */
	public static long toTicks(final long time) {
		return toTicks(time, TimeUnit.MILLISECONDS);
	}

	
	/**
	 * Returns the relative time in ns.
	 * @return the relative time in ms.
	 */
	public static long timens() {
		return System.nanoTime();
	}
	
	/**
	 * Returns the current time in SNMP {@link TimeTicks} equivalent or <b><code>1/100th</code></b> seconds
	 * @return the current time in SNMP {@link TimeTicks} 
	 */
	public static long tick() {
		long tt = time()/100;
		return tt;
	}

	
	
	
	/**
	 * <p>Title: ElapsedTime</p>
	 * <p>Description: An elapsed time reporter</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.utils.time.SystemClock.ElapsedTime</code></p>
	 * TODO: Lots....
	 * Format multiple time units, avg units
	 * Lap times
	 */
	public static class ElapsedTime {
		/** The start time in ns. */
		public final long startNs;
		/** The last lap end time in ns. */
		public long endNs;
	
		/**
		 * Creates a new ElapsedTime
		 */
		private ElapsedTime(){
			startNs = System.nanoTime();
		}
		
		/**
		 * Returns the start time in ns.
		 * @return the start time in ns.
		 */
		public long startTime() {
			return startNs;
		}
		
		/**
		 * Returns the start time in ms.
		 * @return the start time in ms.
		 */
		public long startTimeMs() {
			return TimeUnit.MILLISECONDS.convert(startNs, TimeUnit.NANOSECONDS);
		}
		
		/**
		 * Returns the start time in s.
		 * @return the start time in s.
		 */
		public long startTimeS() {
			return TimeUnit.SECONDS.convert(startNs, TimeUnit.NANOSECONDS);
		}
		
		
		
		
		/** Some extended time unit entries */
		public static final Map<TimeUnit, String> UNITS;
		
		static {
			Map<TimeUnit, String> tmp =  new EnumMap<TimeUnit, String>(TimeUnit.class);
			tmp.put(TimeUnit.DAYS, "days");
			tmp.put(TimeUnit.HOURS, "hrs.");
			tmp.put(TimeUnit.MICROSECONDS, "\u00b5s.");
			tmp.put(TimeUnit.MILLISECONDS, "ms.");
			tmp.put(TimeUnit.MINUTES, "min.");
			tmp.put(TimeUnit.NANOSECONDS, "ns.");
			tmp.put(TimeUnit.SECONDS, "s.");
			UNITS = Collections.unmodifiableMap(tmp);			
		}
		
		
		
//		private ElapsedTime(boolean lap, long endTime) {
//			endNs = endTime;
//			startNs = timerStart.get()[0];
//			long[] lastLapRead = lapTime.get();
//			if(lastLapRead!=null) {
//				lastLapNs = lastLapRead[0];
//			}
//			if(lap) {
//				lapTime.set(new long[]{endTime});
//			} else {
//				timerStart.remove();
//				lapTime.remove();
//			}
//			elapsedNs = endNs-startNs;
//			elapsedMs = TimeUnit.MILLISECONDS.convert(elapsedNs, TimeUnit.NANOSECONDS);
//			if(lastLapNs!=-1L) {
//				elapsedSinceLastLapNs = endTime -lastLapNs;
//				elapsedSinceLastLapMs = TimeUnit.MILLISECONDS.convert(elapsedSinceLastLapNs, TimeUnit.NANOSECONDS);
//			}
//			 
//		}
		/**
		 * Returns the average elapsed time in ms. for the passed number of events
		 * @param cnt The number of events
		 * @return The average elapsed time in ms.
		 */
		public long avgMs(double cnt) {			
			return _avg(elapsed(TimeUnit.MILLISECONDS), cnt);
		}
		
		/**
		 * Returns the average elapsed time in ns. for the passed number of events
		 * @param cnt The number of events
		 * @return The average elapsed time in ns.
		 */
		public long avgNs(double cnt) {
			long elapsedNs = System.nanoTime()-startNs;
			return _avg(elapsedNs, cnt);
		}
		
		
		private static long _avg(double time, double cnt) {
			if(time==0 || cnt==0 ) return 0L;
			double d = time/cnt;
			return Math.round(d);
		}
		
		/**
		 * Returns the elapsed time since start in ns.
		 * @return elapsed ns.
		 */
		public long elapsed() {
			return elapsed(TimeUnit.NANOSECONDS);
		}
		
		/**
		 * Returns the elapsed time since start in ms.
		 * @return elapsed ms.
		 */
		public long elapsedMs() {
			return elapsed(TimeUnit.MILLISECONDS);
		}
		
		/**
		 * Returns the elapsed time since start in s.
		 * @return elapsed s.
		 */
		public long elapsedS() {
			return elapsed(TimeUnit.SECONDS);
		}
		
		/**
		 * Returns the elapsed time since start in the passed unit
		 * @param unit The unit to report elapsed time in
		 * @return the elapsed time
		 */
		public long elapsed(TimeUnit unit) {
			long elapsedNs = System.nanoTime()-startNs;
			if(unit==null) unit = TimeUnit.NANOSECONDS;
			return unit.convert(elapsedNs, TimeUnit.NANOSECONDS);
		}
		
		/**
		 * Returns the decorated elapsed time since start in the passed unit
		 * @param unit The unit to report elapsed time in
		 * @return the decorated elapsed time 
		 */
		public String elapsedStr(TimeUnit unit) {
			long elapsedNs = System.nanoTime()-startNs;
			if(unit==null) unit = TimeUnit.NANOSECONDS;
			return new StringBuilder("[").append(unit.convert(elapsedNs, TimeUnit.NANOSECONDS)).append("] ").append(UNITS.get(unit)).toString();
		}

		/**
		 * Returns the decorated elapsed time since start in ns.
		 * @return the decorated elapsed time since start in ns.
		 */
		public String elapsedStr() {			
			return elapsedStr(TimeUnit.NANOSECONDS);
		}
		
		/**
		 * Returns the decorated elapsed time since start in ms.
		 * @return the decorated elapsed time since start in ms.
		 */
		public String elapsedStrMs() {			
			return elapsedStr(TimeUnit.MILLISECONDS);
		}

		public String printAvg(String unitName, double cnt) {
			endNs = System.nanoTime();
			long elapsedNs = endNs - startNs;
			long avgNs = _avg(elapsedNs, cnt);
			return String.format("Completed %s %s in %s ms.  AvgPer: %s ms/%s \u00b5s/%s ns.",
					(long)cnt,
					unitName, 
					TimeUnit.MILLISECONDS.convert(elapsedNs, TimeUnit.NANOSECONDS),
					TimeUnit.MILLISECONDS.convert(avgNs, TimeUnit.NANOSECONDS),
					TimeUnit.MICROSECONDS.convert(avgNs, TimeUnit.NANOSECONDS),
					avgNs					
			);
		}
		
		public static String printAvg(String unitName, double cnt, final long elapsed, final TimeUnit unit) {
			final long elapsedNs = TimeUnit.NANOSECONDS.convert(elapsed, unit);
			long avgNs = _avg(elapsedNs, cnt);
			return String.format("Completed %s %s in %s ms.  AvgPer: %s ms/%s \u00b5s/%s ns.",
					(long)cnt,
					unitName, 
					TimeUnit.MILLISECONDS.convert(elapsedNs, TimeUnit.NANOSECONDS),
					TimeUnit.MILLISECONDS.convert(avgNs, TimeUnit.NANOSECONDS),
					TimeUnit.MICROSECONDS.convert(avgNs, TimeUnit.NANOSECONDS),
					avgNs					
			);
		}
		
		
	}
	

}
