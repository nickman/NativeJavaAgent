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

import java.util.Collections;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.TreeSet;

/**
 * <p>Title: EntryComparator</p>
 * <p>Description: Comparator support for String/Integer map entry sets</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.agent.EntryComparator</code></p>
 */

public class EntryComparators  {
	/** Ascending Integer comparator */
	public static final Comparator<? super Integer> ASC_INT_COMP = new TreeSet<Integer>().comparator();
	/** Descending Integer comparator */
	public static final Comparator<? super Integer> DESC_INT_COMP = Collections.reverseOrder(ASC_INT_COMP);
	
	/** Ascending Long comparator */
	public static final Comparator<? super Long> ASC_LONG_COMP = new TreeSet<Long>().comparator();
	/** Descending Long comparator */
	public static final Comparator<? super Long> DESC_LONG_COMP = Collections.reverseOrder(ASC_LONG_COMP);
	
	/** Descending long array comparator */
	public static final Comparator<long[]> DESC_LONGARR_COMP = new Comparator<long[]>() {
		@Override
		public int compare(final long[] o1, final long[] o2) {
			return DESC_LONG_COMP.compare(o1[0], o2[0]);
		}
	};
	

	/** Ascending String/Long entry set comparator */
	public static final Comparator<Entry<String, Long>> ASC_ENTRY_STR_LONG_COMP = new AscendingLongEntryComparator<String>();
	/** Descending String/Long entry set comparator */
	public static final Comparator<Entry<String, Long>> DESC_ENTRY_STR_LONG_COMP = new DescendingLongEntryComparator<String>();
	/** Ascending Class/Long entry set comparator */
	public static final Comparator<Entry<Class<?>, Long>> ASC_ENTRY_CLASS_LONG_COMP = new AscendingLongEntryComparator<Class<?>>();
	/** Descending String/Long entry set comparator */
	public static final Comparator<Entry<Class<?>, Long>> DESC_ENTRY_CLASS_LONG_COMP = new DescendingLongEntryComparator<Class<?>>();
	
	/** Wildcard key/long entry set comparator */
	public static final Comparator<Entry<?, Long>> DESC_ENTRY_LONG_COMP = new WildcardDescendingLongEntryComparator();
	
	/** Wildcard key/long[] entry set comparator */
	public static final Comparator<Entry<?, long[]>> DESC_ENTRY_LONGARR_COMP = new WildcardDescendingLongArrayEntryComparator();
	
	
	
	
	/** Ascending String/Integer entry set comparator */
	public static final Comparator<Entry<String, Integer>> ASC_ENTRY_STR_INT_COMP = new AscendingEntryComparator<String>();
	/** Descending String/Integer entry set comparator */
	public static final Comparator<Entry<String, Integer>> DESC_ENTRY_STR_INT_COMP = new DescendingEntryComparator<String>();
	/** Ascending Class/Integer entry set comparator */
	public static final Comparator<Entry<Class<?>, Integer>> ASC_ENTRY_CLASS_INT_COMP = new AscendingEntryComparator<Class<?>>();
	/** Descending String/Integer entry set comparator */
	public static final Comparator<Entry<Class<?>, Integer>> DESC_ENTRY_CLASS_INT_COMP = new DescendingEntryComparator<Class<?>>();

	/** Ascending String/int[] entry set comparator */
	public static final Comparator<Entry<String, int[]>> ASC_ENTRY_STR_INTARR_COMP = new AscendingIntArrEntryComparator<String>();
	/** Descending String/int[] entry set comparator */
	public static final Comparator<Entry<String, int[]>> DESC_ENTRY_STR_INTARR_COMP = new DescendingIntArrEntryComparator<String>();
	/** Ascending Class/int[] entry set comparator */
	public static final Comparator<Entry<Class<?>, int[]>> ASC_ENTRY_CLASS_INTARR_COMP = new AscendingIntArrEntryComparator<Class<?>>();
	/** Descending String/int[] entry set comparator */
	public static final Comparator<Entry<Class<?>, int[]>> DESC_ENTRY_CLASS_INTARR_COMP = new DescendingIntArrEntryComparator<Class<?>>();
	
	
	private static class AscendingIntArrEntryComparator<T> implements Comparator<Entry<T, int[]>> {
		@Override
		public int compare(final Entry<T, int[]> e1, final Entry<T, int[]> e2) {
			return ASC_INT_COMP.compare(e1.getValue()[0], e2.getValue()[0]);
		}
	}
	
	private static class DescendingIntArrEntryComparator<T> implements Comparator<Entry<T, int[]>> {
		@Override
		public int compare(final Entry<T, int[]> e1, final Entry<T, int[]> e2) {
			return DESC_INT_COMP.compare(e1.getValue()[0], e2.getValue()[0]);
		}
	}
	
	
	private static class AscendingEntryComparator<T> implements Comparator<Entry<T, Integer>> {
		/**
		 * {@inheritDoc}
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(final Entry<T, Integer> e1, final Entry<T, Integer> e2) {
			return ASC_INT_COMP.compare(e1.getValue(), e2.getValue());
		}		
	}
	
	private static class DescendingEntryComparator<T> implements Comparator<Entry<T, Integer>> {
		/**
		 * {@inheritDoc}
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(final Entry<T, Integer> e1, final Entry<T, Integer> e2) {
			return DESC_INT_COMP.compare(e1.getValue(), e2.getValue());
		}		
	}
	
	private static class AscendingLongEntryComparator<T> implements Comparator<Entry<T, Long>> {
		/**
		 * {@inheritDoc}
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(final Entry<T, Long> e1, final Entry<T, Long> e2) {
			return ASC_LONG_COMP.compare(e1.getValue(), e2.getValue());
		}		
	}
	
	public static class DescendingLongEntryComparator<T> implements Comparator<Entry<T, Long>> {
		/**
		 * {@inheritDoc}
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(final Entry<T, Long> e1, final Entry<T, Long> e2) {
			return DESC_LONG_COMP.compare(e1.getValue(), e2.getValue());
		}		
	}
	
	private static class WildcardDescendingLongEntryComparator implements Comparator<Entry<?, Long>> {
		/**
		 * {@inheritDoc}
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(final Entry<?, Long> e1, final Entry<?, Long> e2) {
			return DESC_LONG_COMP.compare(e1.getValue(), e2.getValue());
		}		
	}
	
	private static class WildcardDescendingLongArrayEntryComparator implements Comparator<Entry<?, long[]>> {
		/**
		 * {@inheritDoc}
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(final Entry<?, long[]> e1, final Entry<?, long[]> e2) {
			return DESC_LONGARR_COMP.compare(e1.getValue(), e2.getValue());
		}		
	}
	
	
	
	private EntryComparators(){}
	
}
