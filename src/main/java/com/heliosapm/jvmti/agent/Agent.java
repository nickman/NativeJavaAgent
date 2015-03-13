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

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;


/**
 * <p>Title: Agent</p>
 * <p>Description: Native interface class to expose the native-agent's functionality</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.agent.Agent</code></p>
 */

public class Agent {

	public static void main(String[] args) {	
		log("Hello World");
		int a = countInstances(Thread.class);
		log("There are " + a + " instances of " + Thread.class);		
       	Object[] objs = getAllInstances(Thread.class, 3);
       	log("Arr Length:" + objs.length);
       	log("Threads: " + java.util.Arrays.toString(objs));
       	objs = getAllInstances(ThreadPoolExecutor.class, 3);
       	log("Arr Length:" + objs.length);
       	log("TPEs: " + java.util.Arrays.toString(objs));
       	objs = getAllInstances(System.out.getClass(), 300);
       	log("Arr Length:" + objs.length);
       	log("PrintStreams: " + java.util.Arrays.toString(objs));
       	objs = getAllInstances(String.class, 300);
       	log("Arr Length:" + objs.length);
       	log("Strings: " + java.util.Arrays.toString(objs));
       	objs = getAllInstances(String[].class, 300);
       	log("Arr Length:" + objs.length);
       	log("String Arrays: " + java.util.Arrays.deepToString(objs));
       	log("int instance count: %s", countInstances(byte[].class));
       	
	}
	
	public static void log(Object fmt, Object...args) {
		System.out.println(String.format(fmt.toString(), args));
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
	public static int countInstances(Class<?> klass) {
		if(klass==null) throw new IllegalArgumentException("The passed class was null");
		return countInstances0(klass);
	}
	
	/**
	 * Returns an array of instances of the passed class located in the heap
	 * @param klass The class to search and return instances of
	 * @param maxInstances The maximum number of instances to return. A value of zero is equivalent to {@link Integer#MAX_VALUE}.
	 * Negatives values will be rewarded with an {@link IllegalArgumentException}.
	 * @return A [possibly zero length] array of objects
	 */
	public static Object[] getAllInstances(Class<?> klass, int maxInstances) {
		if(klass==null) throw new IllegalArgumentException("The passed class was null");
		if(maxInstances<0) throw new IllegalArgumentException("Invalid maxInstances value [" + maxInstances + "]");
		return getAllInstances0(klass, tagSerial.incrementAndGet(), maxInstances==0 ? Integer.MAX_VALUE : maxInstances);
	}

	/**
	 * Returns an array of instances of the passed class located in the heap, maxing out at {@link Integer#MAX_VALUE} instances.
	 * @param klass The class to search and return instances of
	 * @return A [possibly zero length] array of objects
	 */
	public static Object[] getAllInstances(Class<?> klass) {
		return getAllInstances0(klass, tagSerial.incrementAndGet(), Integer.MAX_VALUE);
	}
		
	private static native int countInstances0(Class<?> klass);
	private static native Object[] getAllInstances0(Class<?> klass, long tag, int maxInstances);
	
}	
	
