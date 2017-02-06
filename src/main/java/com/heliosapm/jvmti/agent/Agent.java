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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.heliosapm.shorthand.attach.vm.VirtualMachine;


/**
 * <p>Title: Agent</p>
 * <p>Description: Native interface class to expose the native-agent's functionality</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.agent.Agent</code></p>
 */

public class Agent {
	/** Indicates if the native library is loaded */
	private static final AtomicBoolean nativeLoaded = new AtomicBoolean(false);
	
//	public static final SetString AGENT_CL = "-agentpath:" "-agentlib:";
	
	public static void main(String[] args) {
//		if(!loadNative()) {
//			System.err.println("Failed to load native");
//			System.exit(-1);
//		}
		log("Hello World");
		//loadNative();
//		iterateInstances0(CharSequence.class, tagSerial.incrementAndGet(), Integer.MAX_VALUE);
//		if(true) return;
		int csCount = countInstances0(CharSequence.class, tagSerial.incrementAndGet(), Integer.MAX_VALUE);
		log("There are " + csCount + " CharSequence instances");
		int a = countExactInstances(Thread.class);
		log("There are " + a + " instances of " + Thread.class);		
       	Object[] objs = getExactInstances(Thread.class);
       	log("Arr Length:" + objs.length);
       	log("Threads: " + java.util.Arrays.toString(objs));
       	objs = getExactInstances(ThreadPoolExecutor.class, 3);
       	log("Arr Length:" + objs.length);
       	log("TPEs: " + java.util.Arrays.toString(objs));
       	objs = getExactInstances(System.out.getClass(), 300);
       	log("Arr Length:" + objs.length);
       	log("PrintStreams: " + java.util.Arrays.toString(objs));
       	objs = getExactInstances(String.class, 300);
       	log("Arr Length:" + objs.length);
       	log("Strings: " + java.util.Arrays.toString(objs));
       	objs = getExactInstances(String[].class, 300);
       	log("Arr Length:" + objs.length);
       	log("String Arrays: " + java.util.Arrays.deepToString(objs));
       	log("int instance count: %s", countExactInstances(byte[].class));
		printClassCardinality(Object.class);
       	// System.out.println("There are " + a + " instances of " + Thread.class);		
       	// Object[] objs = getExactInstances0(Thread.class, System.nanoTime(), 3);       	
       	// System.out.println(objs.length + " Objects: " + java.util.Arrays.toString(objs));

       	
	}
	
	public static void printClassCardinality(final Class<?> type) {
		final Object[] objs = getInstances(type);
		Map<Class<?>, Integer> card = classCardinality(objs);
		log("======== Cardinality for type [%s] ========",type.getName());
		long total = 0;
		for(Map.Entry<Class<?>, Integer> entry: card.entrySet()) {
			log("\t" + entry.getKey().getName() + ":" + entry.getValue());
			total += entry.getValue();
		}
		log("======== Total: [%s]", total);
	}
	
	public static void printExactClassCardinality(final Class<?> type) {
		final Object[] objs = getExactInstances(type);
		Map<Class<?>, Integer> card = classCardinality(objs);
		log("======== Cardinality for exact type [%s] ========",type.getName());
		long total = 0;
		for(Map.Entry<Class<?>, Integer> entry: card.entrySet()) {
			log("\t" + entry.getKey().getName() + ":" + entry.getValue());
			total += entry.getValue();
		}
		log("======== Total: [%s]", total);
	}
	
	
	public static Map<Class<?>, Integer> classCardinality(final Object...instances) {
		final Map<Class<?>, int[]> aggr = new HashMap<Class<?>, int[]>(512);
		for(Object obj: instances) {
			final Class<?> clazz = obj.getClass();
			int[] cnt = aggr.get(clazz);
			if(cnt==null) {
				cnt = new int[]{1};
				aggr.put(clazz, cnt);				
			}
			cnt[0]++;
		}
		final Map<Class<?>, Integer> total = new HashMap<Class<?>, Integer>(aggr.size());
		for(Map.Entry<Class<?>, int[]> entry: aggr.entrySet()) {
			total.put(entry.getKey(), entry.getValue()[0]);
		}
		return total;
	}
	
	public static void log(Object fmt, Object...args) {
		System.out.println(String.format(fmt.toString(), args));
	}
	
	public static boolean loadNative() {
		// FIXME: from sysprop/env
		final String lib = "/home/nwhitehead/hprojects/NativeJavaAgent/target/native/linux_agent_64.so";
		if(nativeLoaded.compareAndSet(false, true)) {
			VirtualMachine vm = null;
			try {
				vm = VirtualMachine.attach(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
				log("VM:" + vm.id() + "\nProps:" + vm.getAgentProperties());
				
				vm.loadAgentPath(lib, null);
			} catch (Throwable t) {
				nativeLoaded.set(false);	
				t.printStackTrace(System.err);
			} finally {
				if(vm!=null) try { vm.detach(); } catch (Exception x) {/* No Op */}
			}
		}
		return nativeLoaded.get();
	}
	
	/** The tag serial */
	private static final AtomicLong tagSerial = new AtomicLong(0L);
	
	public static Class<?> getType(final Class<?> klass) {
		if(klass==null) throw new IllegalArgumentException("The passed class was null");
		if(!klass.isArray()) return klass;
		Class<?> tmp = klass;
		while(tmp.getComponentType()!=null) {
			tmp = tmp.getComponentType();
		}
		return tmp;
	}
	
//	public static boolean isGetCountSupported(final Class<?> klass) {
//		if(klass==null) throw new IllegalArgumentException("The passed class was null");
//	}
	
	/**
	 * Counts the number of instances of the passed class found in the heap
	 * @param klass The class to search for instances of
	 * @return The number of found instances
	 */
	public static int countExactInstances(Class<?> klass) {
		if(klass==null) throw new IllegalArgumentException("The passed class was null");
		return countExactInstances0(klass);
	}
	
//	/**
//	 * Counts the number of instances inherrited from the passed class found in the heap
//	 * @param klass The class to search for instances of
//	 * @return The number of found instances
//	 */
//	public static int countExactInstances(Class<?> klass) {
//		if(klass==null) throw new IllegalArgumentException("The passed class was null");
//		return countExactInstances0(klass);
//	}
	
	
	/**
	 * Returns an array of instances of the passed class located in the heap
	 * @param klass The class to search and return instances of
	 * @param maxInstances The maximum number of instances to return. A value of zero is equivalent to {@link Integer#MAX_VALUE}.
	 * Negatives values will be rewarded with an {@link IllegalArgumentException}.
	 * @return A [possibly zero length] array of objects
	 */
	public static Object[] getExactInstances(Class<?> klass, int maxInstances) {
		if(klass==null) throw new IllegalArgumentException("The passed class was null");
		if(maxInstances<0) throw new IllegalArgumentException("Invalid maxInstances value [" + maxInstances + "]");
		return getExactInstances0(klass, tagSerial.incrementAndGet(), maxInstances==0 ? Integer.MAX_VALUE : maxInstances);
	}

	/**
	 * Returns an array of instances of the passed class located in the heap, maxing out at {@link Integer#MAX_VALUE} instances.
	 * @param klass The class to search and return instances of
	 * @return A [possibly zero length] array of objects
	 */
	public static Object[] getExactInstances(Class<?> klass) {
		return getExactInstances0(klass, tagSerial.incrementAndGet(), Integer.MAX_VALUE);
	}
	
	/**
	 * Returns an array of instances of the passed class located or inherrited, in the heap, maxing out at {@link Integer#MAX_VALUE} instances.
	 * @param klass The class to search and return instances of
	 * @return A [possibly zero length] array of objects
	 */
	public static Object[] getInstances(Class<?> klass) {
		return getInstances0(klass, tagSerial.incrementAndGet(), Integer.MAX_VALUE);
	}
	
		
	private static native int countExactInstances0(Class<?> klass);
	
	private static native int countInstances0(Class<?> klass, long tag, int maxInstances);
	private static native Object[] getExactInstances0(Class<?> klass, long tag, int maxInstances);
	private static native Object[] getInstances0(Class<?> klass, long tag, int maxInstances);
	
}	
	
