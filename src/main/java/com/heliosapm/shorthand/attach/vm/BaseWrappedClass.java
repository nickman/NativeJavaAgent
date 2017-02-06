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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * <p>Title: BaseWrappedClass</p>
 * <p>Description: Base class for reflected access class wrappers</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.attach.vm.BaseWrappedClass</code></p>
 */
public abstract class BaseWrappedClass {
	/** The attach API delegate VirtualMachine */
	protected final Object delegate;
	/** Method mapping synchronization lock */
	protected final Object synchLock = new Object();
	/** The method mapping for this class */
	protected Map<String, Method> methods = null;
	
	/** The reflected class methods keyed by the standard method encoding name */
	protected static final Map<Class<?>, Map<String, Method>> methodMap = new ConcurrentHashMap<Class<?>, Map<String, Method>>();
	
	/** Thread local to save (and restore) a calling thread's context classloader */
	protected static final ThreadLocal<ClassLoader> savedState = new ThreadLocal<ClassLoader>();
	
	/** Static class logger */
	private final Logger log = Logger.getLogger(getClass().getName()); 

	
	/**
	 * Saves the calling thread's context class loader and replaces it with the VM class loader if it's thread local saved state is null
	 */
	public static void pushCl() {
		if(savedState.get()==null) {
			savedState.set(Thread.currentThread().getContextClassLoader());
			ClassLoader cl = VirtualMachineBootstrap.attachClassLoader.get();
			Thread.currentThread().setContextClassLoader(cl==null ? ClassLoader.getSystemClassLoader() : cl);
			//log("Pushed ClassLoader [" + Thread.currentThread().getContextClassLoader() + "]");
		}
	}
	
	/**
	 * Restored the calling thread's context class loader if it's thread local saved state is not null.
	 */
	public static void popCl() {
		if(savedState.get()!=null) {
			Thread.currentThread().setContextClassLoader(savedState.get());
			savedState.remove();
		}
	}
	
	/**
	 * Reflective invocation
	 * @param delegate The target object to invoke against. Ignored if method is static.
	 * @param delegateType The class of the delegate . Ignored if the actual delegate is passed.
	 * @param methodEncode The method encode key
	 * @param args The arguments to pass to the method invocation
	 * @return The return value of the method invocation
	 */
	protected static Object invoke(Object delegate, String delegateType, String methodEncode, Object...args) {
		Method m = null;
		try {
			if(delegate==null && delegateType==null) throw new IllegalArgumentException("The passed delegate and delegate type was null. One must be provided", new Throwable());
			if(methodEncode==null) throw new IllegalArgumentException("The passed methodEncode was null", new Throwable());
			Class<?> delegateClass = null;
	
			if(delegate!=null) {
				delegateClass = delegate.getClass();
			} else {
				delegateClass = VirtualMachineBootstrap.getInstance().classCache.get(delegateType);
			}
			if(delegateClass==null) throw new IllegalArgumentException("Could not determine delegate class", new Throwable());
			Map<String, Method> mMap = getMethodMap(delegateClass);		
			m = mMap.get(methodEncode);
			if(m==null) throw new IllegalArgumentException("The passed methodEncode [" + methodEncode + "] does not map to a delegate method", new Throwable());	
			return m.invoke(java.lang.reflect.Modifier.isStatic(m.getModifiers()) ? null : delegate, args);
		} catch (Exception e) {
			throw new VirtualMachineInvocationException("Failed to invoke [" + (m==null ? methodEncode : m.toGenericString()) + "]", e);
		}
	}
	
	/**
	 * Retrieves the method map for the passed class, climbing the type hierarchy if necessary
	 * @param clazz The class to get the method map for
	 * @return The method map for the passed class
	 */
	protected static Map<String, Method> getMethodMap(Class<?> clazz) {
		Map<String, Method> mMap = null;
		Class<?> target = clazz;
		while(!target.equals(Object.class)) {
			mMap = methodMap.get(target);
			if(mMap!=null) return mMap;
			target = target.getSuperclass();
		}
		throw new IllegalArgumentException("No method map for delegate class [" + clazz.getName() + "]", new Throwable());
	}
	
	/**
	 * Creates a new BaseWrappedClass
	 * @param delegate The attach API delegate object
	 */
	protected BaseWrappedClass(Object delegate) {
		this.delegate = delegate;
		if(methods==null) {
			synchronized(synchLock) {
				if(methods==null) {
					methods = getMethodMapping(this.getClass());
				}
			}
		}		
	}
	
	/**
	 * Creates a new BaseWrappedClass with no delegate
	 */
	protected BaseWrappedClass() {
		this.delegate = null;
		methods = null;
	}

	
	/**
	 * Returns the method mapping for the passed class
	 * @param type The class to get the method mapping for
	 * @return thge method map
	 */
	protected static Map<String, Method> getMethodMapping(Class<?> type) {
		if(type==null) throw new IllegalArgumentException("The passed type was null", new Throwable());		
		Map<String, Method> mMap = methodMap.get(type);
		if(mMap==null) {
			synchronized(type) {
				mMap = methodMap.get(type);
				if(mMap==null) {
					Method[] methods = type.getDeclaredMethods();
					mMap = new HashMap<String, Method>(methods.length);
					methodMap.put(type, mMap);
					Map<String, Integer> overloads = mapOverloads(methods);
					for(Method m: methods) {						
						m.setAccessible(true);
						String name = m.getName();
						if(overloads.get(name)==1 || m.getParameterTypes().length==0) {
							mMap.put(name, m);
						} else {							
							StringBuilder b = new StringBuilder(name);
							for(Class<?> clazz: m.getParameterTypes()) {
								b.append(clazz.isPrimitive() ? clazz.getName().charAt(0) : clazz.getSimpleName().charAt(0));
							}
							mMap.put(b.toString(), m);
						}						
					}
				}
			}			
		}
		return mMap;
	}
	
	
	/**
	 * Returns a map of the method names and a count of the number of instances by each name
	 * @param methods An array of methods
	 * @return A map of overload counts keyed by method name
	 */
	protected static Map<String, Integer> mapOverloads(Method[] methods) {
		Map<String, Integer> map = new HashMap<String, Integer>(methods.length);
		for(Method m: methods) {
			Integer i = map.get(m.getName());
			if(i==null) {
				map.put(m.getName(), 1);
			} else {
				map.put(m.getName(), i+1);
			}
		}
		return map;
	}
	
	/**
	 * Simple out formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public void log(String fmt, Object...args) {
//		System.out.println(String.format(fmt, args));
		log.info(String.format(fmt, args));
	}
	
	/**
	 * Simple err formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public void loge(String fmt, Object...args) {
		//System.err.println(String.format(fmt, args));
		log.severe(String.format(fmt, args));
	}
	
	/**
	 * Simple err formatted logger
	 * @param fmt The format of the message
	 * @param t The throwable to print stack trace for
	 * @param args The message arguments
	 */
	public void loge(String fmt, Throwable t, Object...args) {
		log.log(Level.SEVERE, String.format(fmt, args), t);
//		System.err.println(String.format(fmt, args));
//		t.printStackTrace(System.err);
	}
	
	
	

}
