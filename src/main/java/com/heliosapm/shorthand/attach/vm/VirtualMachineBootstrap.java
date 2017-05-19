/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package com.heliosapm.shorthand.attach.vm;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * <p>Title: VirtualMachineBootstrap</p>
 * <p>Description: Bootstraps the Java Attach API class set</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.attach.vm.VirtualMachineBootstrap</code></p>
 */
public class VirtualMachineBootstrap extends BaseWrappedClass {
	/** The singleton instance  */
	protected static volatile VirtualMachineBootstrap instance = null;
	/** The singleton instance ctor lock */
	protected static final Object lock = new Object();
	/** The class loader used to find the Attach API Jar */
	protected static final AtomicReference<ClassLoader> attachClassLoader = new AtomicReference<ClassLoader>(null);
	/** Static class logger */
	private final static Logger log = Logger.getLogger(VirtualMachineBootstrap.class.getName()); 

	
	
	/** A cache of the reflectively loaded classes keyed by class name */
	protected final Map<String, Class<?>> classCache = new HashMap<String, Class<?>>();

	/** The jar file that usually contains the attach API VirtualMachine classes */
	public static final String JAR_NAME = "tools.jar";
	/** The Java Home location */
	public static final String JAVA_HOME = System.getProperty("java.home");
	/** Alternate locations to look for tools.jar */
	public static final String[] ALT_LOCS = {
		File.separator + ".." + File.separator + "lib" + File.separator
	};
	
	/** Flag indicating if the attach classes have been found */
	private static final AtomicBoolean found = new AtomicBoolean(false);
	
	
	/** The attach API VirtualMachine class name */
	public static final String VM_CLASS = "com.sun.tools.attach.VirtualMachine";
	/** The attach API VirtualMachineDescriptor class name */
	public static final String VM_DESC_CLASS = "com.sun.tools.attach.VirtualMachineDescriptor";
	/** The attach API AttachProvider class name */
	public static final String ATTACH_PROVIDER_CLASS = "com.sun.tools.attach.spi.AttachProvider";
	

	
	/**
	 * Returns the VirtualMachineBootstrap instance
	 * @return the VirtualMachineBootstrap instance
	 */
	public static VirtualMachineBootstrap getInstance() {		
		return getInstance(null);
	}
	
	

	
	/**
	 * Returns the VirtualMachineBootstrap instance
	 * @param urlLocation An optional override fully qualified URL of the attach jar
	 * @return the VirtualMachineBootstrap instance
	 */
	public static VirtualMachineBootstrap getInstance(String urlLocation) {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new VirtualMachineBootstrap(urlLocation);
					try {
						AttachProvider.init();
					} catch (Exception e) {
						e.printStackTrace(System.err);
						throw new RuntimeException("Failed to load Attach API Class. (If you are running a JRE, you need to use a JDK with a tools.jar", e);
					}
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new VirtualMachineBootstrap
	 * @param urlLocation An optional override fully qualified URL of the attach jar
	 */
	protected VirtualMachineBootstrap(String urlLocation) {
		
		findAttachAPI(urlLocation);
		ClassLoader cl = attachClassLoader.get();
		try {
			classCache.put(VM_CLASS, Class.forName(VM_CLASS, true, cl));
			classCache.put(VM_DESC_CLASS, Class.forName(VM_DESC_CLASS, true, cl));
			classCache.put(ATTACH_PROVIDER_CLASS, Class.forName(ATTACH_PROVIDER_CLASS, true, cl));
			BaseWrappedClass.getMethodMapping(classCache.get(VM_CLASS));
			BaseWrappedClass.getMethodMapping(classCache.get(VM_DESC_CLASS));
			BaseWrappedClass.getMethodMapping(classCache.get(ATTACH_PROVIDER_CLASS));
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new RuntimeException("Failed to load Attach API Class. (If you are running a JRE, you need to use a JDK with a tools.jar", e);
		}		
	}
	
	/**
	 * Determines if the passed delegate object is an Attach API class instance and if it is an instance of the named class.
	 * @param obj The delegate object to pass
	 * @param className The class name to test as
	 * @return true if the object is of the passed class.
	 */
	public boolean isInstanceOf(Object obj, String className) {
		if(obj==null) throw new IllegalArgumentException("The passed delegate object was null", new Throwable());
		if(className==null) throw new IllegalArgumentException("The passed class name was null", new Throwable());
		Class<?> clazz = classCache.get(className);
		if(clazz==null) throw new IllegalArgumentException("The passed class name [" + className + "] is not an Attach API class", new Throwable());
		return clazz.isAssignableFrom(obj.getClass());		
	}
	
	
	
	/**
	 * Indicates if the Attach VirtualMachine class can be found in the current classpath
	 * @return true if the Attach VirtualMachine class can be loaded, false if it cannot.
	 */
	protected static boolean inClassPath() {
		return inClassPath(Thread.currentThread().getContextClassLoader());
	}
	
	/**
	 * Indicates if the attach classes have been found
	 * @return true if the attach classes have been found, false otherwise
	 */
	public static boolean isAttachFound() {
		return found.get();
	}
	
	/**
	 * Indicates if the Attach VirtualMachine class can be found in the current classpath
	 * @param classLoader The class loader used to find the Attach API Jar
	 * @return true if the Attach VirtualMachine class can be loaded, false if it cannot.
	 */
	protected static boolean inClassPath(ClassLoader classLoader) {
		if(attachClassLoader.get()!=null) return true;
		try {
			Class<?> clazz = Class.forName(VM_CLASS, true, ClassLoader.getSystemClassLoader());
			attachClassLoader.set(clazz.getClassLoader()==null ? classLoader : clazz.getClassLoader());
			found.set(true);
		} catch (Exception e) {
//			e.printStackTrace(System.err);
		}		
		try {
			Class<?> clazz = Class.forName(VM_CLASS, true, classLoader);
			attachClassLoader.set(clazz.getClassLoader()==null ? classLoader : clazz.getClassLoader());
			found.set(true);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Searches for the Attach API jar
	 */
	protected static void findAttachAPI() {		
		findAttachAPI(null);
	}
	
	/**
	 * Searches for the Attach API jar
	 * @param urlLocation An optional override fully qualified URL of the attach jar
	 */
	protected static void findAttachAPI(String urlLocation) {
		if(found.get()) return;
		//if(inClassPath()) return;
		try {
			Class<?> clazz = Class.forName(VM_CLASS);
//			log.info("Found AttachAPI in Standard ClassPath [" + clazz.getClassLoader() + "]");
			ClassLoader cl = clazz.getClassLoader();
			if(cl==null) {
				cl = ClassLoader.getSystemClassLoader();
			}
//			log.info("Attach API ClassLoader:" + cl);
			attachClassLoader.set(cl);
			found.set(true);
			BaseWrappedClass.savedState.set(null);
			return;
		} catch (Throwable e) {
//			log.info("Not found:" + e);
		}
		List<String> altLocs = new ArrayList<String>();
		if(urlLocation!=null) {
			altLocs.add(urlLocation);
		}
		for(String s: ALT_LOCS) {
//			System.out.println("ALT_LOC: [" + (JAVA_HOME + s + JAR_NAME) + "]");
			altLocs.add(JAVA_HOME + s + JAR_NAME);
		}
		for(String s: altLocs) {
			try {
				File toolsLoc = new File(s);
				//log.info("Testing [" + toolsLoc + "]");
				if(toolsLoc.exists()) {
					URL url = toolsLoc.toURI().toURL();
					URLClassLoader ucl = new URLClassLoader(new URL[]{url}, ClassLoader.getSystemClassLoader().getParent());
					if(inClassPath(ucl)) {
						//log.info("Attach API Found And Loaded [" + toolsLoc + "]");	
//						attachClassLoader.set(ucl);
//						BaseWrappedClass.savedState.set(null);						
						return;
					}
				}
			} catch (Exception e) {				
			}
		}	
		if(attachClassLoader.get()==null) {
			throw new RuntimeException("Failed to find the Attach API. Please add tools.jar to the classpath", new Throwable());
		}
	}
	
	/**
	 * Quickie command line test
	 * @param args None
	 */
	public static void main(String[] args) {
		findAttachAPI();
		log.info("VMBoot:" + inClassPath());
		getInstance();		
//		for(VirtualMachineDescriptor vmd: VirtualMachine.list()) {
//			try {
//				VirtualMachine vm = VirtualMachine.attach(vmd);
//				log.info("\tVM:" + vm.toString());
//			} catch (Exception e) {
//				log.info("Unable to attach to VM [" + vmd.toString() + "]:" + e);
//			}
//		}
//		for(VirtualMachineDescriptor vmd: VirtualMachineDescriptor.getVirtualMachineDescriptors()) {
//			try {
//				VirtualMachine vm = VirtualMachine.attach(vmd);
//				log.info("\tVM:" + vm.toString());
//			} catch (Exception e) {
//				log.info("Unable to attach to VM [" + vmd.toString() + "]:" + e);
//			}
//		}
//		AttachProvider ap = AttachProvider.getAttachProviders().iterator().next();
//		String id = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
//		VirtualMachine vm = ap.attachVirtualMachine(id);
//		log.info("This VM:" + vm.toString());
		AttachProvider ap = AttachProvider.getAttachProviders().iterator().next();
		String id = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		for(VirtualMachineDescriptor vmd: ap.listVirtualMachines()) {
			log.info("Testing VMD (" + vmd.id() + ") [" + vmd.displayName() + "]   Name:" + vmd.provider().name() + "  Type:" + vmd.provider().type());
			if(id.equals(vmd.id())) {
				VirtualMachine vm = ap.attachVirtualMachine(vmd);
				log.info("This VM:" + vm.toString());	
				Properties agentProps = vm.getAgentProperties();
				for(Map.Entry<Object, Object> p: agentProps.entrySet()) {
					log.info("\t\t" + p.getKey() + ":" + p.getValue());
				}
			}
			if(vmd.id().equals("15684")) {
//				log.info("==============  System Props  ==============");
//				Properties sysProps = ap.attachVirtualMachine(vmd).getSystemProperties();
//				for(Map.Entry<Object, Object> p: sysProps.entrySet()) {
//					log.info("\t\t" + p.getKey() + ":" + p.getValue());
//				}
				log.info("==============  Agent Props  ==============");
				Properties agentProps = ap.attachVirtualMachine(vmd).getAgentProperties();
				for(Map.Entry<Object, Object> p: agentProps.entrySet()) {
					log.info("\t\t" + p.getKey() + ":" + p.getValue());
				}
				
			}				
			
		}
		
//		BaseWrappedClass.getMethodMapping(vmb.classCache.get(VM_CLASS));
//		for(VirtualMachineDescriptor vmd: VirtualMachine.list()) {
//			log.info(vmd.toString());
//		}
		
	}
	
}
