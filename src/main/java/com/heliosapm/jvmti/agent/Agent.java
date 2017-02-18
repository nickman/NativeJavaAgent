/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.heliosapm.jvmti.agent;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.management.ObjectName;

import com.heliosapm.jvmti.util.TimerHistory;


/**
 * <p>Title: Agent</p>
 * <p>Description: Native interface class to expose the native-agent's functionality</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.agent.Agent</code></p>
 */

public class Agent implements AgentMBean {
	/** The singleton instance */
	private static volatile Agent instance = null;
	/** The singleton instance ctor lock */
	private static Object lock = new Object();
	/** Signature for an array */
	public static final String ARRAY_IND = "[]";
	/** Empty int[] arr placeholder */
	public static int[] INT_ARR_PLACEHOLDER = {};
	/** Empty long[] arr placeholder */
	public static long[] LONG_ARR_PLACEHOLDER = {};
	
	private final NativeAgent nativeAgent;
	private final TimerHistory topNTimerHistory;
	
	
	/**
	 * Acquires the singleton agent instance
	 * @return the agent
	 */
	public static Agent getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new Agent();
				}
			}
		}
		return instance;
	}
	
	/** The system property key to override the default JMX ObjectName for the agent */
	public static final String AGENT_OBJECT_NAME_PROP = "com.heliosapm.jvmti.agent.objectname";
	/** The default JMX ObjectName for the agent */
	public static final String DEFAULT_AGENT_OBJECT_NAME = "com.heliosapm.jvmti:service=Agent";
	
	private Agent() {
		nativeAgent = NativeAgent.getInstance();
		topNTimerHistory = nativeAgent.topNTimerHistory();
		ObjectName objectName = null;
		try {
			objectName = new ObjectName(System.getProperty(AGENT_OBJECT_NAME_PROP, DEFAULT_AGENT_OBJECT_NAME));
		} catch (Exception ex) {
			System.err.println("Failed to build object name from configured ObjectName [" + System.getProperty(AGENT_OBJECT_NAME_PROP) + "]. Reverting to default.");
			try {
				objectName = new ObjectName(DEFAULT_AGENT_OBJECT_NAME);
			} catch (Exception x) {/* No Op */}
		}
		try {
			ManagementFactory.getPlatformMBeanServer().registerMBean(this, objectName);
		} catch (Exception ex) {
			System.err.println("Failed to register Agent management interface:" + ex);
		}
	}
	
	
	
	public static void main(String[] args) {
		log("Hello World");
		final Agent agent = Agent.getInstance();
//		final Object[] foos = new Object[100];
//		final Object[] bars = new Object[100];
//		for(int i = 0; i < 100; i++) {
//			foos[i] = new Foo();
//			bars[i] = new Bar();
//		}
//		log("Was Loaded:" + agent.wasLoaded());
//		int csCount = agent.getInstanceCount(CharSequence.class);
//		log("There are " + csCount + " CharSequence instances");
//		int a = agent.getInstanceCount(Thread.class);
//		log("There are " + a + " instances of " + Thread.class);		
//       	Object[] objs = agent.getInstancesOf(Thread.class);
//       	log("Arr Length:" + objs.length);
//       	log("Threads: " + java.util.Arrays.toString(objs));
//       	objs = agent.getInstancesOf(ThreadPoolExecutor.class, 3);
//       	log("Arr Length:" + objs.length);
//       	log("TPEs: " + java.util.Arrays.toString(objs));
//       	objs = agent.getInstancesOf(System.out.getClass(), 300);
//       	log("Arr Length:" + objs.length);
//       	log("PrintStreams: " + java.util.Arrays.toString(objs));
//       	objs = agent.getInstancesOf(String.class, 300);
//       	log("Arr Length:" + objs.length);
//       	log("Strings: " + java.util.Arrays.toString(objs));
//       	objs = agent.getInstancesOf(String[].class, 300);
//       	log("Arr Length:" + objs.length);
//       	log("String Arrays: " + java.util.Arrays.deepToString(objs));
//       	log("int instance count: %s", agent.getExactInstanceCount(byte[].class));
//       	
//       	log("==== Types of charsequence");
//       	for(Class<?> clazz: agent.getAllTypesOf(CharSequence.class)) {
//       		log("\t%s", clazz.getName());
//       	}
//       	
//       	final int loops = 20000;
//       	final ElapsedTime et = SystemClock.startClock();
//       	int max = 0;
//       	int min = Integer.MAX_VALUE;
//       	int maxLoop = -1;
//       	int minLoop = -1;
//       	for(int i = 0; i < loops; i++) {
//       		Object[] all = agent.getInstancesOfAny(Object.class);
//       		final int count = all.length;
//       		if(count > max) {
//       			max = count;
//       			maxLoop = i;
//       		}
//       		if(count < min) {
//       			min = count;
//       			minLoop = i;
//       		}
//       		all = null;
//       		if(i%1000==0) System.gc();
//       	}
//       	log(et.printAvg("AllObjects Lookup", loops));
//       	log("Max: %s at loop %s, Min: %s at loop %s", max, maxLoop, min, minLoop);
       	
       	//agent.printClassCardinality(Object.class);
		
//		final int TOPN = 100;		
//		log("Loaded:" + agent.isAgentBootLoaded());
//		int cnt = 0;
//		for(int i = 0; i < 100; i++) {
//			cnt += agent.getTopNInstanceCounts("java.lang.Object", TOPN, true).size();
//			System.gc();
//			cnt += agent.getTopNInstanceCounts(Object.class, TOPN, true).size();
//			System.gc();
//		}
//		log("Cnt:" + cnt);
//		
//		System.gc();
//		
//		log("Total Objects Before:" + agent.getInstanceCountOfAny(Object.class));
//		
//		System.gc();
//		
//		final ElapsedTime et2 = SystemClock.startClock();
//		final Map<Class<Object>, Long> map2 = agent.getTopNInstanceCounts(Object.class, TOPN, false);
//		final long elapsed2 = et2.elapsed();
//		final long count2 = map2.values().parallelStream().mapToLong(l -> l.longValue()).sum();
//		log(ElapsedTime.printAvg("Examined Objects By Class", count2, elapsed2, TimeUnit.NANOSECONDS));
//		
//		System.gc();
//
//		final ElapsedTime et = SystemClock.startClock();
//		final Map<String, Long> map = agent.getTopNInstanceCounts("java.lang.Object", TOPN, false);
//		final long elapsed = et.elapsed();
//		final long count = map.values().parallelStream().mapToLong(l -> l.longValue()).sum();
//		log(ElapsedTime.printAvg("Examined Objects By ClassName", count, elapsed, TimeUnit.NANOSECONDS));
		final int TOPN = 10;
		int gtot = 0;
		for(int i = 0; i < 100; i++) {
			gtot +=  agent.getTopNInstanceCounts(Object.class, TOPN, true).size();
			gtot +=  agent.getTopNInstanceCounts(Object.class.getName(), TOPN, true).size();
			System.gc();
		}
		
		System.gc();
		//final Map<String, Long> map = agent.getTopNInstanceCounts("java.lang.Object", TOPN, true);
		long start = System.currentTimeMillis();
		final Map<Class<Object>, Long> map = agent.getTopNInstanceCounts(Object.class, TOPN, true);
		long elapsed = System.currentTimeMillis() - start;
		log("\n\t==========================\n\tTop %s\n\t==========================", TOPN);
		for(Map.Entry<Class<Object>, Long> entry: map.entrySet()) {
			log("%s  :  %s", entry.getKey().getName(), entry.getValue());
		}
		log("\n\t==========================");
		log("Size:" + map.size() + ", elapsed:" + elapsed);
		System.gc();
		start = System.currentTimeMillis();
		final Map<String, Long> map2 = agent.getTopNInstanceCounts(Object.class.getName(), TOPN, true);
		elapsed = System.currentTimeMillis() - start;
		log("\n\t==========================\n\tTop %s\n\t==========================", TOPN);
		for(Map.Entry<String, Long> entry: map2.entrySet()) {
			log("%s  :  %s", entry.getKey(), entry.getValue());
		}
		log("\n\t==========================");
		log("Size:" + map.size() + ", elapsed:" + elapsed);
		
//		SystemClock.sleep(999999999);
//		int objArrayCount = agent.getExactInstanceCount("java.lang.Object[]");
//		log("Object[] count: %s", objArrayCount);
	}
	
	
	
	
	public static void log(Object fmt, Object...args) {
		System.out.println(String.format(fmt.toString(), args));
	}
	
	/**
	 * Returns all loaded types equal to or inherrited from the passed type
	 * @param type The type to retrieve
	 * @return a set of classes
	 */
	public Set<Class<?>> getAllTypesOf(final Class<?> type) {
		return Arrays.stream(getInstancesOfAny(type))
		.parallel()
		.map(o -> o.getClass())
		.collect(Collectors.toCollection(HashSet<Class<?>>::new));
	}
	
	/**
	 * Returns all type names equal to or inherrited from the passed type 
	 * for which there are instances in the heap
	 * @param type The type to retrieve
	 * @return a set of class names
	 */
	public Set<String> getAllTypeNamesOf(final Class<?> type) {
		return Arrays.stream(getInstancesOfAny(type))
		.parallel()
		.map(o -> renderClassName(o.getClass()))
		.collect(Collectors.toCollection(HashSet<String>::new));
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jvmti.agent.AgentMBean#getNativeLibrary()
	 */
	@Override
	public String getNativeLibrary() {		
		return nativeAgent.getNativeLibrary();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jvmti.agent.AgentMBean#isAgentBootLoaded()
	 */
	@Override
	public boolean isAgentBootLoaded() {		
		return nativeAgent.isAgentLoadedAtBoot();
	}

	/** Predicate to filter out primtive types and arrays of primitive types */
	public final Predicate<Class<?>> noPrimitiveClassFilter = new Predicate<Class<?>>() {
		@Override
		public boolean test(final Class<?> fklazz) {				
			return !(fklazz.isPrimitive() || (fklazz.isArray() && getComponentClass(fklazz).isPrimitive()));
		}
	};
	/** No Op Predicate class filter */
	public final Predicate<Class<?>> noOpClassFilter = new Predicate<Class<?>>() {
		@Override
		public boolean test(final Class<?> fklazz) {			
			return true;
		}
	}; 
	/** Predicate to filter out primtive types and arrays of primitive types */
	public final Predicate<Map.Entry<Class<?>, long[]>> noPrimitiveEntrySetFilter = new Predicate<Map.Entry<Class<?>, long[]>>() {
		@Override
		public boolean test(final Entry<Class<?>, long[]> entry) {
			final Class<?> fklazz = entry.getKey();
			return !(fklazz.isPrimitive() || (fklazz.isArray() && getComponentClass(fklazz).isPrimitive()));
		}
	};
	/** No Op Predicate class filter */
	public final Predicate<Map.Entry<Class<?>, long[]>> noOpEntrySetFilter = new Predicate<Map.Entry<Class<?>, long[]>>() {
		@Override
		public boolean test(final Entry<Class<?>, long[]> entry) {
			return true;
		}
	};
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jvmti.agent.AgentMBean#getTopNInstanceCounts(java.lang.String, int, boolean)
	 */
	@Override
	public LinkedHashMap<String, Long> getTopNInstanceCounts(final String className, final int n, final boolean excludePrims) {
		if(className==null || className.trim().isEmpty()) throw new IllegalArgumentException("The passed class name was null or empty");
		if(n<1) throw new IllegalArgumentException("Invalid max instances:" + n);		
		final ConcurrentHashMap<String, long[]> mMap = new ConcurrentHashMap<String, long[]>(n > 8192 ? 8192 : n);
		final LinkedHashMap<String, Long> topMap = new LinkedHashMap<String, Long>(n > 8192 ? 8192 : n);
		for(final Class<?> clazz: resolveClass(className)) {
			nativeAgent.getInstanceCardinality(clazz).entrySet().parallelStream()
				.forEach(entry -> {
					if(!excludePrims || noPrimitiveClassFilter.test(entry.getKey())) {
						final String key = renderClassName(entry.getKey());
						long[] count = mMap.putIfAbsent(key, LONG_ARR_PLACEHOLDER);
						if(count==null || count==LONG_ARR_PLACEHOLDER) {
							mMap.replace(key, LONG_ARR_PLACEHOLDER, entry.getValue());
						} else {
							count[0] += entry.getValue()[0];
						}
					}
				});
		}
		mMap.entrySet().stream()
			.sorted(EntryComparators.DESC_ENTRY_LONGARR_COMP)
			.limit(n)
			.forEach(entry -> {topMap.put(entry.getKey(), entry.getValue()[0]);});
		
		
		
//		resolveClass(className)
//			.parallelStream()			
//			.map(c -> getInstancesOfAny(c))
//			.flatMap(arr -> { 
//				return Arrays.stream(arr)
//					.parallel()					
//					.map(Object::getClass)
//					.filter(excludePrims ? noPrimitiveClassFilter : noOpClassFilter)
//					.map(k -> renderClassName(k));				
//			})
//			.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
//			.entrySet().stream().sorted(EntryComparators.DESC_ENTRY_STR_LONG_COMP)
//			.limit(n)
//			.forEachOrdered(e -> topMap.put(e.getKey(), e.getValue()));
		return topMap;
	}
	
	/**
	 * Returns the top <code>N</code> classes by count
	 * @param clazz The class to count instances for
	 * @param n The top n value
	 * @param excludePrims exclude primitives and arrays of primitives
	 * @return A map of the number of class instances keyed by the class
	 */
	@SuppressWarnings("unchecked")
	public <T> LinkedHashMap<Class<T>, Long> getTopNInstanceCounts(final Class<T> clazz, final int n, final boolean excludePrims) {
		if(clazz==null) throw new IllegalArgumentException("The passed class was null");
		if(n<1) throw new IllegalArgumentException("Invalid max instances:" + n);
		final Map<Class<?>, long[]> card = nativeAgent.getInstanceCardinality(clazz);
		
		final LinkedHashMap<Class<T>, Long> topMap = new LinkedHashMap<Class<T>, Long>(n > 8192 ? 8192 : n);
		card.entrySet().parallelStream()
			.filter(excludePrims ? noPrimitiveEntrySetFilter : noOpEntrySetFilter)
			.sorted(EntryComparators.DESC_ENTRY_LONGARR_COMP)
			.limit(n)
			.forEachOrdered(e -> topMap.put((Class<T>) e.getKey(), e.getValue()[0]));
		return topMap;
//		Arrays.stream(getInstancesOfAny(clazz))
//			.parallel()					
//			.map(Object::getClass)
//			.filter(excludePrims ? noPrimitiveClassFilter : noOpClassFilter)
//			.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
//			.entrySet().stream().sorted(EntryComparators.DESC_ENTRY_LONG_COMP)
//			.limit(n)
//			.forEachOrdered(e -> topMap.put((Class<T>) e.getKey(), e.getValue()));
		
	}
	
	
	/**
	 * Finds the component class for an array type
	 * @param clazz The array type
	 * @return the base component class for the passed type
	 */
	public Class<?> getComponentClass(final Class<?> arrayType) {
		return getComponentClass(arrayType, null);
	}
	
	/**
	 * Finds the component class for an array type 
	 * and puts the dimension of the type in the passed array at index 0
	 * @param arrayType The array type
	 * @param dimension The optional array that the type's array dimension is put into.
	 * Ignored if null or zero length.
	 * @return the base component class for the passed type
	 */
	public static Class<?> getComponentClass(final Class<?> arrayType, final int[] dimension) {
		if(arrayType==null) throw new IllegalArgumentException("The passed class was null");		
		if(!arrayType.isArray()) return arrayType;
		final boolean dimTrack = dimension!=null && dimension.length > 0;
		int dim = 0;
		Class<?> current = arrayType;
		while(current.isArray()) { 
			current = current.getComponentType();
			dim++;
		}
		if(dimTrack) dimension[0] = dim;
		return current;
	}
	
	/**
	 * Counts the number of instances of the passed class found in the heap
	 * @param klass The class to search for instances of
	 * @return The number of found instances
	 */
	public int getInstanceCountOf(Class<?> klass) {
		if(klass==null) throw new IllegalArgumentException("The passed class was null");
		return nativeAgent.getInstanceCountOf(klass);
	}
	
	/**
	 * Returns the number of instances of the passed class 
	 * or any that implement or inherrit from it 
	 * @param className The name of the class to count instances for
	 * @return the number of instances found
	 */
	public int getInstanceCountOfAny(final Class<?> klass) {
		if(klass==null) throw new IllegalArgumentException("The passed class was null");
		return nativeAgent.getInstanceCountOfAny(klass);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jvmti.agent.AgentMBean#getInstanceCountOf(java.lang.String)
	 */
	@Override
	public int getInstanceCountOf(final String className) {
		if(className==null || className.trim().isEmpty()) throw new IllegalArgumentException("The passed class name was null or empty");
		int total = 0;
		for(Class<?> clazz: resolveClass(className)) {
			total += getInstanceCountOf(clazz);
		}
		return total;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jvmti.agent.AgentMBean#getInstanceCountOfAny(java.lang.String)
	 */
	@Override
	public int getInstanceCountOfAny(String className) {
		int total = 0;
		for(Class<?> clazz: resolveClass(className)) {
			total += getInstanceCountOfAny(clazz);
		}
		return total;
	}
	
	
	private Set<Class<?>> resolveClass(final String className) {
		int arrIndex = className.indexOf(ARRAY_IND);		
		if(arrIndex!=-1) {
			int dimension = 0;
			final String baseClass = className.substring(0, arrIndex);
			while(arrIndex!=-1) {
				dimension++;
				arrIndex = className.indexOf(ARRAY_IND, arrIndex+1);
			}
			final Set<Class<?>> baseClasses = resolveClass(baseClass);
			final Set<Class<?>> arrayClasses = new HashSet<Class<?>>(baseClasses.size());
			for(Class<?> bClass : baseClasses) {
				arrayClasses.add(Array.newInstance(bClass, new int[dimension]).getClass());
			}
			return arrayClasses;
		}
		
		final Set<Class<?>> resolved = new HashSet<Class<?>>();
		for(ClassLoader cl : getInstancesOfAny(ClassLoader.class)) {
			try {
				resolved.add(cl.loadClass(className));
			} catch (Throwable ex) {/* No Op */}
		}
		return resolved;
	}
	
//	private Set<String> resolveClassToNames(final String className) {
//		int arrIndex = className.indexOf(ARRAY_IND);		
//		if(arrIndex!=-1) {
//			int dimension = 0;
//			final String baseClass = className.substring(0, arrIndex);
//			while(arrIndex!=-1) {
//				dimension++;
//				arrIndex = className.indexOf(ARRAY_IND, arrIndex+1);
//			}
//			final Set<Class<?>> baseClasses = resolveClass(baseClass);
//			final Set<String> arrayClasses = new HashSet<String>(baseClasses.size());
//			for(Class<?> bClass : baseClasses) {
//				arrayClasses.add(renderClassName(Array.newInstance(bClass, new int[dimension]).getClass()));
//			}
//			return arrayClasses;
//		}
//		
//		final Set<String> resolved = new HashSet<String>();
//		for(ClassLoader cl : getInstancesOfAny(ClassLoader.class)) {
//			try {
//				resolved.add(renderClassName(cl.loadClass(className)));
//			} catch (Throwable ex) {/* No Op */}
//		}
//		return resolved;
//	}
	
	
	public static String renderClassName(final Class<?> clazz) {
		if(clazz==null) throw new IllegalArgumentException("The passed class was null");
		if(clazz.isArray()) {
			final int[] dimension = new int[]{0};
			final Class<?> base = getComponentClass(clazz, dimension);
			final StringBuilder b = new StringBuilder(base.getName());
			for(int i = 0; i < dimension[0]; i++) {
				b.append(ARRAY_IND);
			}
			return b.toString();
		}
		return clazz.getName();
	}
	
	public Set<Class<?>> resolveArrayClassName(final String className) {
		if(className.length()<3) throw new RuntimeException("Cannot resolve array class name [" + className + "]");
		int dimensions = 0;
		final StringBuilder b = new StringBuilder(className);
		char last = b.charAt(b.length()-1);
		char plast = b.charAt(b.length()-2);
		while(last==']' && plast=='[') {
			dimensions++;
			b.deleteCharAt(b.length()-1); b.deleteCharAt(b.length()-1);			
		}
		final String component = b.toString();
		final Set<Class<?>> componentClasses = resolveClass(component);
		final Set<Class<?>> resolvedClasses = new HashSet<Class<?>>();
		for(Class<?> klazz : componentClasses) {
			try {
				resolvedClasses.add(Array.newInstance(klazz, new int[dimensions]).getClass());
			} catch (Exception x) {/* No Op */}
		}
		return resolvedClasses;
	}
	
	/**
	 * Returns an array of instances of the passed class located in the heap
	 * @param klass The class to search and return instances of
	 * @param maxInstances The maximum number of instances to return. A value of zero is equivalent to {@link Integer#MAX_VALUE}.
	 * Negatives values will be rewarded with an {@link IllegalArgumentException}.
	 * @return A [possibly zero length] array of objects
	 */
	@SuppressWarnings("unchecked")
	public <T> T[] getInstancesOf(final Class<T> klass, final int maxInstances) {
		if(klass==null) throw new IllegalArgumentException("The passed class was null");
		if(maxInstances<0) throw new IllegalArgumentException("Invalid maxInstances value [" + maxInstances + "]");
		return (T[])nativeAgent.getInstancesOf(klass, maxInstances); 
	}

	/**
	 * Returns an array of instances of the passed class located in the heap, maxing out at {@link Integer#MAX_VALUE} instances.
	 * @param klass The class to search and return instances of
	 * @return A [possibly zero length] array of objects
	 */
	public <T> T[] getInstancesOf(final Class<T> klass) {
		return getInstancesOf(klass, Integer.MAX_VALUE);
	}
	
	/**
	 * Returns an array of instances of the passed class located or inherrited, in the heap, maxing out at {@link Integer#MAX_VALUE} instances.
	 * @param klass The class to search and return instances of
	 * @param maxInstances The maximum number of instances to return
	 * @return A [possibly zero length] array of objects
	 */
	@SuppressWarnings("unchecked")
	public <T> T[] getInstancesOfAny(final Class<T> klass, final int maxInstances) {
		return (T[]) nativeAgent.getInstancesOfAny(klass, maxInstances); 
	}
	
	/**
	 * Returns an array of instances of the passed class located or inherrited, in the heap, maxing out at {@link Integer#MAX_VALUE} instances.
	 * @param klass The class to search and return instances of
	 * @return A [possibly zero length] array of objects
	 */
	public <T> T[] getInstancesOfAny(final Class<T> klass) {
		return getInstancesOfAny(klass, Integer.MAX_VALUE);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jvmti.agent.AgentMBean#getTopNLast()
	 */
	@Override
	public long getTopNLast() {
		return topNTimerHistory.last();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jvmti.agent.AgentMBean#getTopNCount()
	 */
	@Override
	public long getTopNCount() {
		return topNTimerHistory.count();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jvmti.agent.AgentMBean#getTopNAverage()
	 */
	@Override
	public double getTopNAverage() {
		return topNTimerHistory.average();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jvmti.agent.AgentMBean#getTopNMax()
	 */
	@Override
	public long getTopNMax() {
		return topNTimerHistory.max();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jvmti.agent.AgentMBean#getTopNMin()
	 */
	@Override
	public long getTopNMin() {
		return topNTimerHistory.min();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jvmti.agent.AgentMBean#resetTimers()
	 */
	@Override
	public void resetTimers() {
		topNTimerHistory.reset();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jvmti.agent.AgentMBean#getTopNStats()
	 */
	@Override
	public LongSummaryStatistics getTopNStats() {
		return topNTimerHistory.stats();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jvmti.agent.AgentMBean#resetTimersAll()
	 */
	@Override
	public void resetTimersAll() {
		topNTimerHistory.resetAll();
		
	}
	
//	/**
//	 * Prints the count of each type equal to or inherrited 
//	 * from the passed type found in the heap
//	 * @param type The type to search the heap for
//	 * @return a string listing the found types and count for each
//	 */
//	public String printCardinalityOfAny(final Class<?> type) {
//		final Object[] objs = getInstancesOfAny(type);
//		Map<Class<?>, Integer> card = classCardinality(objs);
//		final StringBuilder b = new StringBuilder(card.size() * 20);		
//		for(Map.Entry<Class<?>, Integer> entry: card.entrySet()) {
//			if(b.length() > 0) {
//				b.append("\n");
//			}
//			b.append(entry.getKey().getName()).append(":").append(entry.getValue());			
//		}
//		return b.toString();		
//	}
	
}	
	
