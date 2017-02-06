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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	/** The command line agent load prefixed */
	public static final Set<String> AGENT_CL_PREFIX = 
			Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("-agentpath:", "-agentlib:", "-javaagent:")));
	/** The agent native lib name */
	public static final String LIB_NAME = "oif_agent";
	/** The system property override defining the absolute location of the oif agent library to load */
	public static final String CONFIG_AGENT_LOCATION = "com.heliosapm.jvmti.lib";
	/** The directory prefix when loading the default lib in dev mode */
	public static final String DEV_DIR_PREFIX = "target/native/";
	/** This JVM's process id */
	public static final String PID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
	/** The tag serial */
	private final AtomicLong tagSerial = new AtomicLong(0L);
	
	
	public static void main(String[] args) {
		final Agent agent = new Agent();
		log("Hello World");
		agent.loadNative();
		log("Was Loaded:" + agent.wasLoaded());
		int csCount = countInstances0(CharSequence.class, agent.tagSerial.incrementAndGet(), Integer.MAX_VALUE);
		log("There are " + csCount + " CharSequence instances");
		int a = agent.countExactInstances(Thread.class);
		log("There are " + a + " instances of " + Thread.class);		
       	Object[] objs = agent.getExactInstances(Thread.class);
       	log("Arr Length:" + objs.length);
       	log("Threads: " + java.util.Arrays.toString(objs));
       	objs = agent.getExactInstances(ThreadPoolExecutor.class, 3);
       	log("Arr Length:" + objs.length);
       	log("TPEs: " + java.util.Arrays.toString(objs));
       	objs = agent.getExactInstances(System.out.getClass(), 300);
       	log("Arr Length:" + objs.length);
       	log("PrintStreams: " + java.util.Arrays.toString(objs));
       	objs = agent.getExactInstances(String.class, 300);
       	log("Arr Length:" + objs.length);
       	log("Strings: " + java.util.Arrays.toString(objs));
       	objs = agent.getExactInstances(String[].class, 300);
       	log("Arr Length:" + objs.length);
       	log("String Arrays: " + java.util.Arrays.deepToString(objs));
       	log("int instance count: %s", agent.countExactInstances(byte[].class));
       	agent.printClassCardinality(Object.class);
	}
	
	public void printClassCardinality(final Class<?> type) {
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
	
	public void printExactClassCardinality(final Class<?> type) {
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
	
	
	public Map<Class<?>, Integer> classCardinality(final Object...instances) {
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
	
	/**
	 * Determines the os and cpu arch from system properties and returns the 
	 * directory and library file name
	 * @return the directory and library file name for this platform
	 */
	public String libDir() {
		final String os = System.getProperty("os.name").toLowerCase();
		final String arch = System.getProperty("sun.arch.data.model");
		if(os.contains("linux")) {
			if(arch.equals("64")) {
				return "linux64/oif_agent.so";
			} else if(arch.equals("32")) {
				return "linux32/oif_agent.so";
			} else {
				throw new RuntimeException("Linux arch Not Supported: [" + arch + "]");
			}
		} else if(os.contains("windows")) {
			if(arch.equals("64")) {
				return "win64/oif_agent.dll";
			} else if(arch.equals("32")) {
				return "win32/oif_agent.dll";
			} else {
				throw new RuntimeException("Windows arch Not Supported: [" + arch + "]");
			}			
		} else {
			throw new RuntimeException("OS Not Supported: [" + os + "]");
		}
		
	}
	
	/**
	 * Determines if the agent was loaded on the command line
	 * @return true if loaded on command line, false otherwise
	 */
	public boolean wasLoadedCl() {
		final List<String> clArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
		for(final String arg: clArgs) {
			for(final String prefix: AGENT_CL_PREFIX) {
				if(arg.startsWith(prefix)) {
					if(arg.contains(LIB_NAME)) {
						nativeLoaded.set(true);
						return true;
					}
				}
			}
		}
		return false;
	}
	
	
	/**
	 * Attempts to load the oif agent if it is determined it is not already loaded
	 * @return true if the agent is loaded, false otherwise (which means it failed to load)
	 */
	public boolean loadNative() {
		if(wasLoadedCl()) return true;
		if(nativeLoaded.compareAndSet(false, true)) {
			VirtualMachine vm = null;
			try {
				// Check for sysprop override
				final String libToLoad;
				if(System.getProperties().contains(CONFIG_AGENT_LOCATION)) {
					libToLoad = System.getProperties().getProperty(CONFIG_AGENT_LOCATION);									
				} else {
					// determine if we're running in a jar (true) or dev mode (false)
					final boolean isJar = Agent.class.getProtectionDomain().getCodeSource().getLocation().toString().endsWith(".jar");					
					if(isJar) {
						libToLoad = unloadLibFromJar().getAbsolutePath();
					} else {
						libToLoad = "./target/native/" + libDir();
					}
				}
				loadLibFromFile(libToLoad);
			} catch (Throwable t) {
				nativeLoaded.set(false);	
				t.printStackTrace(System.err);
			} finally {
				if(vm!=null) try { vm.detach(); } catch (Exception x) {/* No Op */}
			}
		}
		return nativeLoaded.get();
	}
	
	/**
	 * Acquires a VirtualMachine instance, executes the passed task, and dettaches
	 * @param task The task to run against the VirtualMachine
	 * @return the return value of the task
	 * @throws Exception the exception thrown from the task
	 */
	public <T> T runInVirtualMachine(final VirtualMachineTask<T> task) throws Exception {
		VirtualMachine vm = null;
		try {
			vm = VirtualMachine.attach(PID);
			task.setVirtualMachine(vm);
			return task.call();
		} catch (Throwable t) {
			if(Exception.class.isInstance(t)) throw (Exception)t;
			throw new RuntimeException(t);
		} finally {
			if(vm!=null) try { vm.detach(); } catch (Exception x) {/* No Op */}
		}
	}
	
	/**
	 * Unloads the platform library from the native agent jar and writes it to a temp file
	 * @return the temp file the library was written to
	 * @throws Exception thrown on any error
	 */
	private File unloadLibFromJar() throws Exception {
		final ClassLoader cl = Agent.class.getClassLoader();
		final String resourceName = libDir();
		final String libFileName = resourceName.split("/")[1];
		final InputStream is = cl.getResourceAsStream(resourceName);
		if(is==null) throw new Exception("Failed to find resource [" + resourceName + "]");
		FileOutputStream fos = null;
		final byte[] transferBuffer = new byte[8192];
		int bytesRead = -1;
		try {
			final File libFile = File.createTempFile(LIB_NAME, libFileName);
			libFile.deleteOnExit();
			fos = new FileOutputStream(libFile);
			while((bytesRead = is.read(transferBuffer))!=-1) {
				fos.write(transferBuffer, 0, bytesRead);
			}
			fos.flush();
			return libFile;
		} finally {
			if(is!=null) try { is.close(); } catch (Exception x) {/* No Op */}
			if(fos!=null) try { fos.close(); } catch (Exception x) {/* No Op */}
			
		}
	}
	
	/**
	 * Loads the native library from the passed file
	 * @param fileName The name of the agent lib file to load 
	 */
	public void loadLibFromFile(final String fileName) {
		final File absFile = new File(new File(fileName).getAbsolutePath());
		if(!absFile.exists()) {
			throw new RuntimeException("Failed to find lib file [" + absFile + "]");			
		}
		try {
			runInVirtualMachine(new AbstractVirtualMachineTask<Void>(){
				@Override
				public Void call() throws Exception {
					vm.loadAgentPath(absFile.getAbsolutePath(), null);
					return null;
				}
			});
		} catch (Exception ex) {
			throw new RuntimeException("Failed to load lib file [" + fileName + "]", ex);
		}
		
	}
	
	
	public Class<?> getType(final Class<?> klass) {
		if(klass==null) throw new IllegalArgumentException("The passed class was null");
		if(!klass.isArray()) return klass;
		Class<?> tmp = klass;
		while(tmp.getComponentType()!=null) {
			tmp = tmp.getComponentType();
		}
		return tmp;
	}
	
	/**
	 * Counts the number of instances of the passed class found in the heap
	 * @param klass The class to search for instances of
	 * @return The number of found instances
	 */
	public int countExactInstances(Class<?> klass) {
		if(klass==null) throw new IllegalArgumentException("The passed class was null");
		return countExactInstances0(klass);
	}
	
	/**
	 * Returns an array of instances of the passed class located in the heap
	 * @param klass The class to search and return instances of
	 * @param maxInstances The maximum number of instances to return. A value of zero is equivalent to {@link Integer#MAX_VALUE}.
	 * Negatives values will be rewarded with an {@link IllegalArgumentException}.
	 * @return A [possibly zero length] array of objects
	 */
	public Object[] getExactInstances(Class<?> klass, int maxInstances) {
		if(klass==null) throw new IllegalArgumentException("The passed class was null");
		if(maxInstances<0) throw new IllegalArgumentException("Invalid maxInstances value [" + maxInstances + "]");
		return getExactInstances0(klass, tagSerial.incrementAndGet(), maxInstances==0 ? Integer.MAX_VALUE : maxInstances);
	}

	/**
	 * Returns an array of instances of the passed class located in the heap, maxing out at {@link Integer#MAX_VALUE} instances.
	 * @param klass The class to search and return instances of
	 * @return A [possibly zero length] array of objects
	 */
	public Object[] getExactInstances(Class<?> klass) {
		return getExactInstances0(klass, tagSerial.incrementAndGet(), Integer.MAX_VALUE);
	}
	
	/**
	 * Returns an array of instances of the passed class located or inherrited, in the heap, maxing out at {@link Integer#MAX_VALUE} instances.
	 * @param klass The class to search and return instances of
	 * @return A [possibly zero length] array of objects
	 */
	public Object[] getInstances(Class<?> klass) {
		return getInstances0(klass, tagSerial.incrementAndGet(), Integer.MAX_VALUE);
	}
	
	/**
	 * Indicates if the agent was loaded or attached
	 * @return true if loaded, false if attached
	 */
	public boolean wasLoaded() {
		return wasLoaded0();
	}
	
		
	private static native int countExactInstances0(Class<?> klass);	
	private static native int countInstances0(Class<?> klass, long tag, int maxInstances);
	private static native Object[] getExactInstances0(Class<?> klass, long tag, int maxInstances);
	private static native Object[] getInstances0(Class<?> klass, long tag, int maxInstances);
	private static native boolean wasLoaded0();
}	
	
