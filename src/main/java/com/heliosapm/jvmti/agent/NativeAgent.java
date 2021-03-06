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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jctools.maps.NonBlockingHashMap;
import org.jctools.maps.NonBlockingHashMapLong;
import org.jctools.queues.SpscGrowableArrayQueue;
import org.pmw.tinylog.Logger;

import com.heliosapm.jvmti.util.SystemClock;
import com.heliosapm.jvmti.util.SystemClock.ElapsedTime;
import com.heliosapm.jvmti.util.TimerHistory;
import com.heliosapm.shorthand.attach.vm.VirtualMachine;

/**
 * <p>Title: NativeAgent</p>
 * <p>Description: The native agent providing the core JVMTI operations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.agent.NativeAgent</code></p>
 */

public class NativeAgent {
	/** The singleton instance */
	private static volatile NativeAgent instance = null;
	/** The singleton instance ctor lock */
	private static Object lock = new Object();
	/** Indicates if the native library is loaded */
	private static final AtomicBoolean nativeLoaded = new AtomicBoolean(false);
	/** The command line agent load prefixed */
	public static final Set<String> AGENT_CL_PREFIX = 
			Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("-agentpath:", "-agentlib:", "-javaagent:")));
	/** The agent native lib name */
	public static final String LIB_NAME = "oif_agent";
	/** The number of cores */
	public static final int CORES = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	/** The system property override defining the absolute location of the oif agent library to load */
	public static final String CONFIG_AGENT_LOCATION = "com.heliosapm.jvmti.lib";
	/** The system property override to suppress the deleteOnExit on the jar extracted lib. Just set, no value needed. */
	public static final String CONFIG_AGENT_NO_DEL_LIB = "com.heliosapm.jvmti.lib.nodelonexit";
	/** The sys prop for the library path */
	public static final String SYS_LIB_PATH = "java.library.path";
	/** The platform path separator */
	public static final String PATH_SEPARATOR = File.pathSeparator;
	/** System property set in target JVM when agent is installed */
	public static final String AGENT_INSTALLED_PROP = "com.heliosapm.jvmti.agent.installed";
	/** This JVM's process id */
	public static final String PID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
	/** The directory prefix when loading the default lib in dev mode */
	public static final String DEV_DIR_PREFIX = "target/native/";
	/** Class Cardinality counter map */
	private final NonBlockingHashMapLong<NonBlockingHashMap<Class<?>, long[]>> classCounter = new NonBlockingHashMapLong<NonBlockingHashMap<Class<?>, long[]>>(CORES, true);
	/** Class Cardinality timer map */
	private final NonBlockingHashMapLong<ElapsedTime> classCountTimer = new NonBlockingHashMapLong<ElapsedTime>(CORES, true);
	
	/** Thread pool to dispatch queued response native JVMTI calls */
	private final ExecutorService threadPool =  Executors.newWorkStealingPool(CORES);
	
	/** long array place holder */
	private static final long[] PLACEHOLDER = {}; 
	
	private static final EOQ END_OF_QUEUE = new EOQ();
	
	
	/** The tag serial */
	private final AtomicLong tagSerial = new AtomicLong(1L);
	/** The native library loaded */
	private String libLocation = null;
	/** The top n timer history */
	private final TimerHistory topNTimerHistory = new TimerHistory(1000);

	/**
	 * Acquires the singleton NativeAgent instance
	 * @return the agent
	 */
	public static NativeAgent getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new NativeAgent(true);
				}
			}
		}
		return instance;
	}
	
	
	private NativeAgent(final boolean loadNative) {
		if(loadNative) {
			loadNative();
		}
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
				Logger.info("Loading native library for Linux/64");
				return "linux64/liboifagent.so";
			} else if(arch.equals("32")) {
				Logger.info("Loading native library for Linux/32");
				return "linux32/liboifagent.so";
			} else {
				throw new RuntimeException("Linux arch Not Supported: [" + arch + "]");
			}
		} else if(os.contains("windows")) {
			if(arch.equals("64")) {
				Logger.info("Loading native library for Windows/64");
				return "win64/oifagent.dll";
			} else if(arch.equals("32")) {
				Logger.info("Loading native library for Windows/32");
				return "win32/oifagent.dll";
			} else {
				throw new RuntimeException("Windows arch Not Supported: [" + arch + "]");
			}			
		} else {
			throw new RuntimeException("OS Not Supported: [" + os + "]");
		}		
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
				if(System.getProperties().containsKey(CONFIG_AGENT_LOCATION)) {
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
				libLocation = libToLoad;
				initCallbacks0(this, SpscGrowableArrayQueue.class, END_OF_QUEUE);
				System.setProperty(AGENT_INSTALLED_PROP, "true");
				Logger.info("Loaded native library [{}]", libLocation);
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
			fos = new FileOutputStream(libFile);
			while((bytesRead = is.read(transferBuffer))!=-1) {
				fos.write(transferBuffer, 0, bytesRead);
			}
			fos.flush();
			fos.close();
			if(!System.getProperties().containsKey(CONFIG_AGENT_NO_DEL_LIB)) {
				libFile.deleteOnExit();
			}
			
			return libFile;
		} finally {
			if(is!=null) try { is.close(); } catch (Exception x) {/* No Op */}
			if(fos!=null) try { fos.close(); } catch (Exception x) {/* No Op */}
			
		}
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
	
	protected <T> Stream<T> streamResults(final int maxSize) {
		final SpscGrowableArrayQueue<T> spscQ = new SpscGrowableArrayQueue<T>(128, maxSize);
		spscQ.offer(null);
		return spscQ.stream();
		
	}

	/**
	 * Returns the name and location of the native library
	 * @return the name and location of the native library
	 */
	public String getNativeLibrary() {		
		return libLocation;
	}
	
	/**
	 * Indicates if the agent was loaded at boot time or attached
	 * @return true if the agent was loaded at boot time, false if attached
	 */
	public boolean isAgentLoadedAtBoot() {		
		return wasLoaded();
	}
	
	
	/**
	 * Appends the passed path to the java library search path
	 * @param path The path to append
	 */
	public void appendToLibPath(final String path) {
		final String current = System.getProperty(SYS_LIB_PATH);
		final String newPath = (current==null ? "" : (current + PATH_SEPARATOR)) + path;
		System.setProperty(SYS_LIB_PATH, newPath);
		resetLibPath();
	}
	
	/**
	 * Resets the internal library path so it can be redefined
	 */
	private static void resetLibPath() {
		try {
			final Field fieldSysPath = ClassLoader.class.getDeclaredField( "sys_paths" ); 
			fieldSysPath.setAccessible( true ); 
			fieldSysPath.set( null, null ); 
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public static final int NON_CONCRETE_MODIFIERS = Modifier.ABSTRACT | Modifier.INTERFACE;
	
	/**
	 * Determines if the passed class is one for which the heap may contain
	 * multiple instances. i.e. excludes interface and abstract classes
	 * @param klazz The class to test
	 * @return true if concrete, false otherwise
	 */
	public static boolean isConcrete(final Class<?> klazz) {
		if(klazz==null) throw new IllegalArgumentException("The passed class was null");
		final int mods = klazz.getModifiers();
		return !(Modifier.isInterface(mods) || Modifier.isAbstract(mods));
	}
	
	
	public void countCallback(final Class<?> clazz) {
		Logger.info("Instance Found: [{}]", clazz.getName());
	}
	
	/**
	 * Returns a count of instances in the heap of or inherrited from the passed class
	 * @param klazz The class to get instance counts for
	 * @return A map of instance counts keyed by the class
	 */
	public Map<Class<?>, long[]> getInstanceCardinality(final Class<?> klazz) {
		if(klazz==null) throw new IllegalArgumentException("The passed class was null");
		final long tag = tagSerial.incrementAndGet();
		classCounter.put(tag, new NonBlockingHashMap<Class<?>, long[]>());
		classCountTimer.put(tag, SystemClock.startClock());
		typeCardinality0(klazz, tag, Integer.MAX_VALUE);
		return classCounter.remove(tag);
	}

	//============================================================================
	//	Cardinality Callbacks
	//============================================================================
	
	/**
	 * Callback from the native lib when running {@link #typeCardinality0(Class, long, int)}.
	 * Accumulates the count of each type of object tagged.
	 * @param tag The tag used for object tagging
	 * @param obj The tagged object
	 */
	public void increment(final long tag, final Object obj) {
		if(obj==null) return;
		final Class<?> clazz = obj.getClass();
		final NonBlockingHashMap<Class<?>, long[]> map = classCounter.get(tag);
		long[] counter = map.putIfAbsent(clazz, PLACEHOLDER);
		if(counter==null || counter==PLACEHOLDER) {
			counter = new long[]{1L};
			map.replace(clazz, PLACEHOLDER, counter);
		} else {
			counter[0]++;			
		}
	}
	
	/**
	 * Callback from the native lib when {@link #typeCardinality0(Class, long, int)} is complete.
	 * @param tag The tag used for object tagging
	 */
	public void complete(final long tag) {
		final ElapsedTime et = classCountTimer.get(tag);
		if(et!=null) {
			topNTimerHistory.add(et.elapsed(TimeUnit.MILLISECONDS));
		}
	}
	
	/**
	 * Returns the topn timer history
	 * @return the topn timer history
	 */
	public TimerHistory topNTimerHistory() {
		return topNTimerHistory;
	}
	
	//============================================================================
	//	Native JVMTI call wrappers
	//============================================================================

	/**
	 * Indicates if the agent was loaded or attached.
	 * Useful as a basic test for the agent presence.
	 * @return true if loaded, false if attached
	 */
	public boolean wasLoaded() {
		try {
			return wasLoaded0();
		} catch (Throwable t) {
			return false;
		}
	}
	
	private static final Object[] EMPTY_ARR = {};
	
	/**
	 * Counts the number of heap objects of the exact passed type
	 * @param exactType The exact type of heap objects to count
	 * @return the number of objects found on the heap
	 */
	public int getInstanceCountOf(final Class<?> exactType) {
		if(exactType==null) throw new IllegalArgumentException("The passed class was null");
		if(!isConcrete(exactType)) return 0;
		return countExactInstances0(exactType, tagSerial.incrementAndGet(), Integer.MAX_VALUE);
	}
	
	/**
	 * Counts the number of heap objects of the passed type or any type inherrited from it
	 * @param anyType The type of heap objects to count
	 * @return the number of objects found on the heap
	 */
	public int getInstanceCountOfAny(final Class<?> anyType) {
		if(anyType==null) throw new IllegalArgumentException("The passed class was null");
		return countInstances0(anyType, tagSerial.incrementAndGet(), Integer.MAX_VALUE);
	}
	
	/**
	 * Returns heap objects of the exact passed type
	 * @param exactType The exact type of heap objects to return
	 * @param maxInstances The maximum number of instances
	 * @return an array of objects found in the heap
	 */
	@SuppressWarnings("unchecked")
	public <T> T[] getInstancesOf(final Class<T> exactType, final int maxInstances) {
		if(exactType==null) throw new IllegalArgumentException("The passed class was null");
		if(!isConcrete(exactType)) return (T[])EMPTY_ARR;
		return (T[])getExactInstances0(exactType, tagSerial.incrementAndGet(), maxInstances);		
	}
	
	protected <T> Queue<T> queueInstancesOf(final Class<T> exactType, final int queueSize) {
		if(exactType==null) throw new IllegalArgumentException("The passed class was null");		
		if(!isConcrete(exactType)) return (Queue<T>)EMPTY_QUEUE;
		final SpscGrowableArrayQueue<Object> queue = new SpscGrowableArrayQueue<Object>(128);
		threadPool.submit(new Runnable(){
			public void run() {
				queueExactInstances0(exactType, tagSerial.incrementAndGet(), queueSize, queue);
			}
		});		
		return (Queue<T>)queue;		
	}
	
	public <T> void instancesOf(final Class<T> exactType, final int maxInstances, final Consumer<T> consumer) {
		final Queue<T> q = queueInstancesOf(exactType, maxInstances);
		final int breakOn = maxInstances + 1;
		try {
			Object x = null;
			int c = 0;
			while(true) {
				x = q.poll();
				if(x==null) continue;
				if(x==END_OF_QUEUE) {
					break;
				}
				c++;
				if(c > maxInstances) break;
				consumer.accept((T)x);
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}		
		
	}
	
	private static native <T> int queueExactInstances0(Class<?> klass, long tag, int maxInstances, SpscGrowableArrayQueue<T> queue);
	private static final ConstQueue<Object> EMPTY_QUEUE = new ConstQueue<Object>();
	
	public static int nextPowerOf2(final int i) {
		return Math.max(1, Integer.highestOneBit(i - 1) << 1);
	}
	
	/**
	 * Returns heap objects of the passed type or any type inherrited from it
	 * @param anyType The type of heap objects to return
	 * @param maxInstances The maximum number of instances
	 * @return an array of objects found in the heap
	 */
	@SuppressWarnings("unchecked")
	public <T> T[] getInstancesOfAny(final Class<?> anyType, final int maxInstances) {
		if(anyType==null) throw new IllegalArgumentException("The passed class was null");
		return (T[])getInstances0(anyType, tagSerial.incrementAndGet(), maxInstances);		
	}
	
	public Map<Class<?>, long[]> typeCardinality(final Class<?> type, final long tag, final int maxInstances) {
		//topNTimerHistory.add(classCountTimer.remove(tag).elapsed(TimeUnit.MILLISECONDS));
		classCountTimer.put(tag, SystemClock.startClock());
		classCounter.put(tag, new NonBlockingHashMap<Class<?>, long[]>());
		int types = 0;
		try {
			typeCardinality0(type, tag, maxInstances);
			Map<Class<?>, long[]> counts = classCounter.remove(tag);
			types = counts.size();
			return counts;
		} finally {
			ElapsedTime et = classCountTimer.remove(tag);
			if(et!=null) Logger.info("Elapsed: {}", et.printAvg("Per Type", types));
			classCounter.remove(tag);
		}
	}
	
	public static void main(String[] args) {
		Logger.info("Initializing....");
		final NativeAgent na = getInstance();
		Logger.info("Initialized.");
		final long tag = na.tagSerial.incrementAndGet();
		na.classCounter.put(tag, new NonBlockingHashMap<Class<?>, long[]>());
		final long start = System.currentTimeMillis();
		Map<Class<?>, long[]> counts = na.typeCardinality(Object.class, tag, Integer.MAX_VALUE);
		Logger.info("Elapsed: {} ms.", (System.currentTimeMillis() - start));
		final LongAdder cnt = new LongAdder();
		for(int i = 0; i < 100; i++) {
			na.instancesOf(String.class, 1000, a -> {
				cnt.increment();
			});
			cnt.reset();
		}
		cnt.reset();
		final ElapsedTime et = SystemClock.startClock();
		na.instancesOf(Object.class, 1000, a -> {
			cnt.increment();
		});
		Logger.info("Counted {} objects in {} ms.", cnt.sum(), et.elapsedMs());
	}
	

	//============================================================================
	//	Native JVMTI calls
	//============================================================================
	private static native int countExactInstances0(Class<?> klass, long tag, int maxInstances);	
	private static native int countInstances0(Class<?> klass, long tag, int maxInstances);
	private static native Object[] getExactInstances0(Class<?> klass, long tag, int maxInstances);
//	private static native int queueExactInstances0(Class<?> klass, long tag, int maxInstances, SpscGrowableArrayQueue<Object> queue);
	private static native Object[] getInstances0(Class<?> klass, long tag, int maxInstances);
	private static native boolean wasLoaded0();
	private static native boolean initCallbacks0(Object callbackSite, Class<SpscGrowableArrayQueue> queueClazz, Object endOfQueue);
	private static native void typeCardinality0(Class<?> targetClass, long tag, int maxInstances);
	
	
	private static class EOQ {
		@Override
		public String toString() {
			return "{END-OF-QUEUE}";
		}
	}
	
	private static class ConstQueue<T> implements Queue<T> {
		private final Set<T> emptySet = Collections.unmodifiableSet(new HashSet<T>(0));

		/* (non-Javadoc)
		 * @see java.util.Collection#size()
		 */
		@Override
		public int size() {
			return 0;
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#isEmpty()
		 */
		@Override
		public boolean isEmpty() {
			return true;
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#contains(java.lang.Object)
		 */
		@Override
		public boolean contains(Object o) {
			return false;
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#iterator()
		 */
		@Override
		public Iterator<T> iterator() {
			return emptySet.iterator();
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#toArray()
		 */
		@Override
		public Object[] toArray() {
			return EMPTY_ARR;
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#toArray(java.lang.Object[])
		 */
		@Override
		public <T> T[] toArray(T[] a) {
			return (T[])EMPTY_ARR;
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#remove(java.lang.Object)
		 */
		@Override
		public boolean remove(Object o) {
			return false;
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#containsAll(java.util.Collection)
		 */
		@Override
		public boolean containsAll(Collection<?> c) {
			return false;
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#addAll(java.util.Collection)
		 */
		@Override
		public boolean addAll(Collection<? extends T> c) {
			return false;
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#removeAll(java.util.Collection)
		 */
		@Override
		public boolean removeAll(Collection<?> c) {
			return false;
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#retainAll(java.util.Collection)
		 */
		@Override
		public boolean retainAll(Collection<?> c) {
			return false;
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#clear()
		 */
		@Override
		public void clear() {
		}

		/* (non-Javadoc)
		 * @see java.util.Queue#add(java.lang.Object)
		 */
		@Override
		public boolean add(T e) {
			return false;
		}

		/* (non-Javadoc)
		 * @see java.util.Queue#offer(java.lang.Object)
		 */
		@Override
		public boolean offer(T e) {
			return false;
		}

		/* (non-Javadoc)
		 * @see java.util.Queue#remove()
		 */
		@Override
		public T remove() {
			return null;
		}

		/* (non-Javadoc)
		 * @see java.util.Queue#poll()
		 */
		@Override
		public T poll() {
			return null;
		}

		/* (non-Javadoc)
		 * @see java.util.Queue#element()
		 */
		@Override
		public T element() {
			return null;
		}

		/* (non-Javadoc)
		 * @see java.util.Queue#peek()
		 */
		@Override
		public T peek() {
			return null;
		}
		
	}
	
	
}
