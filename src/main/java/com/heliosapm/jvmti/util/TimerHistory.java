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

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.LongSummaryStatistics;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

import com.heliosapm.jvmti.util.SystemClock.ElapsedTime;

/**
 * <p>Title: TimerHistory</p>
 * <p>Description: A fixed size long time manager</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.util.TimerHistory</code></p>
 */

public class TimerHistory {
	/** The long array holder */
	private final LongBuffer arr;
	/** The max size of the array */
	private final int maxSize;
	/** The compact position */
	private final int compactPos;
	/** The total number of added points */
	private final AtomicLong count = new AtomicLong(0L);
	
	/** The current size */
	private final AtomicInteger size = new AtomicInteger(0);
	/** The concurrency guard */
	private final AtomicLong guard = new AtomicLong(-1L);

	/**
	 * Creates a new TimerHistory
	 * @param size The size of the array
	 */
	public TimerHistory(final int size) {
		if(size < 1) throw new IllegalArgumentException("Invalid size:" + size);
		maxSize = size;
		compactPos = maxSize-1;
		arr = ByteBuffer.allocateDirect(size * 8).asLongBuffer();		
	}
	
	private static final long[] EMPTY_ARR = {};
	private static final LongStream EMPTY_STREAM = LongStream.empty();
	
	private long[] asArray() {
		final int s = size.get();
		if(s==0) return EMPTY_ARR;
		final long[] larr = new long[s];
		arr.position(0);
		arr.slice().get(larr, 0, Math.min(s, maxSize));
		return larr;
	}
	
	private void lock(final boolean barge) {
		final long id = Thread.currentThread().getId();
		while(true) {
			if(guard.compareAndSet(-1L, id)) break;
			if(!barge) Thread.yield();
		}
	}
	
	private void unlock() {
		final long id = Thread.currentThread().getId();
		if(guard.get()!=id) throw new IllegalStateException("Not locked by calling thread");
		guard.set(-1L);
	}
	
	/*
	 * [][][]
	 * [a][][]
	 * [a][b][]
	 * [a][b][c]
	 * [b][c][d]
	 * [c][d][e]
	 */
	
	public static void log(Object fmt, Object...args) {
		System.out.println(String.format(fmt.toString(), args));
	}

	
	public void add(final long value) {
		try {
			lock(false);
			final int s = size.get();
			if(s < maxSize) {
				arr.put(s, value);
				size.incrementAndGet();
			} else {
				arr.position(1);
				arr.compact();
				arr.put(compactPos, value);
			}
			count.incrementAndGet();
		} finally {
			unlock();
		}
	}
	
	public LongStream stream() {
		try {
			lock(false);
			if(size.get()==0) return EMPTY_STREAM;
			return LongStream.of(asArray()).parallel();
		} finally {
			unlock();
		}
	}
	
	public long count() {
		return count.get();
	}
	
	public double average() {
		return stream().average().orElse(-1L);
	}
	
	public long max() {
		return stream().max().orElse(-1L);
	}
	
	public long min() {
		return stream().min().orElse(-1L);
	}
	
	public LongSummaryStatistics stats() {
		return stream().summaryStatistics();
	}
	
	
	public long last() {
		try {
			lock(false);
			if(size.get()==0) return -1L;
			return arr.get(0);
		} finally {
			unlock();
		}		
	}
	
	/**
	 * Resets the array but not the count
	 */
	public void reset() {
		try {
			size.set(0);
			lock(true);
		} finally {
			unlock();
		}
	}
	
	/**
	 * Resets the array and the count
	 */
	public void resetAll() {
		try {
			size.set(0);
			count.set(0L);
			lock(true);
		} finally {
			unlock();
		}
	}
	
	
	
	public static void main(String[] args) {
		final ThreadLocalRandom r = ThreadLocalRandom.current();
		final long[] samples = new long[1000];
		for(int i = 0; i < 1000; i++) {
			samples[i] = Math.abs(r.nextLong(1000));
		}
		final TimerHistory t = new TimerHistory(1000);
		for(int i = 0; i < 100000; i++) {
			t.add(Math.abs(r.nextLong(1000)));
		}
		t.reset();
		for(int i = 0; i < 1000; i++) {
			t.add(samples[i]);
		}
		final ElapsedTime et = SystemClock.startClock();
		for(int x = 0; x < 10; x++) {
			for(int i = 0; i < 1000; i++) {
				t.add(samples[i]);
			}			
		}
		log(et.printAvg("Samples", 10000));
		log("Average : %s", (long)t.stream().average().getAsDouble());
		log("Distinct : %s", t.stream().distinct().toArray().length);
		log("First : %s", t.stream().findFirst().getAsLong());
		log("Max : %s", t.stream().max().getAsLong());
		log("Min : %s", t.stream().min().getAsLong());
		log("Sum : %s", t.stream().sum());
		log("Count : %s", t.count());
		log("Summary : %s", t.stream().summaryStatistics());
	}
	
	
	public String toString() {
		final long[] larr;
		try {
			lock(true);
			larr = asArray();			
		} finally {
			unlock();
		}
		return arr.toString() + ":" + Arrays.toString(larr);
	}
	
	
}
