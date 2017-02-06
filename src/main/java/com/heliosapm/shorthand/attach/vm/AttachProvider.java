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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;



/**
 * <p>Title: AttachProvider</p>
 * <p>Description: Wrapper class for Attach API's {@link com.sun.tools.attach.spi.AttachProvider}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.attach.vm.AttachProvider</code></p>
 */
public class AttachProvider extends BaseWrappedClass {
	/** A map of attach provider delegates keyed by their system identity hash codes */
	private static final Map<Integer, AttachProvider> apInstances = new ConcurrentHashMap<Integer, AttachProvider>();
	/** Static class logger */
	private final static Logger log = Logger.getLogger(AttachProvider.class.getName()); 
	
	/**
	 * Returns a collection of all known attach providers
	 * @return a collection of attach providers
	 */
	public static Collection<AttachProvider> getAttachProviders() {
		return Collections.unmodifiableCollection(apInstances.values());
	}
	
	/**
	 * Acquires the wrapped AttachProvider for the passed delegate
	 * @param delegate The AttachProvider delegate object
	 * @return a wrapped AttachProvider 
	 */
	public static AttachProvider getInstance(Object delegate) {
		if(delegate==null) throw new IllegalArgumentException("The passed AttachProvider delegate was null", new Throwable());
		if(!VirtualMachineBootstrap.getInstance().isInstanceOf(delegate, VirtualMachineBootstrap.ATTACH_PROVIDER_CLASS)) {
			throw new IllegalArgumentException("The passed delegate of type [" + delegate.getClass().getName() + "] was not of the type [" + VirtualMachineBootstrap.ATTACH_PROVIDER_CLASS + "]", new Throwable());
		}		
		int id = System.identityHashCode(delegate);
		AttachProvider ap = apInstances.get(id);
		if(ap==null) {
			synchronized(apInstances) {
				ap = apInstances.get(id);
				if(ap==null) {
					ap = new AttachProvider(delegate);
					apInstances.put(id, ap);
				}
			}
		}
		return ap;
	}
	
	/**
	 * Lists the Java virtual machines known to this provider. 
	 * @return The list of virtual machine descriptors which describe the Java virtual machines known to this provider (may be empty).
	 */
	public List<VirtualMachineDescriptor> listVirtualMachines() {
		List<VirtualMachineDescriptor> results = new ArrayList<VirtualMachineDescriptor>();
		try {
			pushCl();
			List<?> vmds = (List<?>)invoke(delegate, null, "listVirtualMachines");
			for(Object vmd: vmds) {
				results.add(VirtualMachineDescriptor.getInstance(vmd));
			}
		} finally {
			popCl();
		}
		return results;
	}
	
	/**
	 * Attaches to a Java virtual machine. 
	 * @param id The abstract identifier that identifies the Java virtual machine. 
	 * @return VirtualMachine representing the target virtual machine. 
	 */
	public VirtualMachine attachVirtualMachine(String id) {
		try {
			pushCl();
			return VirtualMachine.getInstance(invoke(delegate, null, "attachVirtualMachineS", id));
		} finally {
			popCl();
		}		
	}
	
	/**
	 * Attaches to a Java virtual machine. 
	 * @param vmd The virtual machine descriptor 
	 * @return VirtualMachine representing the target virtual machine. 
	 */
	public VirtualMachine attachVirtualMachine(VirtualMachineDescriptor vmd) {
		try {
			pushCl();
			return VirtualMachine.getInstance(invoke(delegate, null, "attachVirtualMachineV", vmd.delegate));
		} finally {
			popCl();
		}		
	}
	
	/**
	 * Return this provider's name. 
	 * @return This provider's name 
	 */
	public String name() {
		try {
			pushCl();
			return (String)invoke(delegate, null, "name");
		} finally {
			popCl();
		}		
	}	
	
	/**
	 * Return this provider's type. 
	 * @return this provider's type. 
	 */
	public String type() {
		try {
			pushCl();
			return (String)invoke(delegate, null, "type");			
		} finally {
			popCl();
		}
	}	
	
	/**
	 * Returns a list of the installed attach providers. 
	 * @return A list of the installed attach providers.
	 */
	public static List<AttachProvider> providers() {
		return Collections.unmodifiableList(new ArrayList<AttachProvider>(getAttachProviders()));
	}
	
	/**
	 * Initializes the list of know attach providers
	 */
	static void init() {
		VirtualMachineBootstrap.findAttachAPI();
		try {
			pushCl();			
//			log.info("Loading Attach Provider with [" + Thread.currentThread().getContextClassLoader() + "]");
			Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(VirtualMachineBootstrap.ATTACH_PROVIDER_CLASS);
			Method m = clazz.getDeclaredMethod("providers");
			m.setAccessible(true);
			List<?> aps = (List<?>)m.invoke(null);
			for(Object del: aps) {
				getInstance(del);
			}			
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize AttachProvider Cache", e);
		} finally {
			popCl();
		}
	}
	
	/**
	 * Creates a new AttachProvider
	 * @param delegate the delegate object
	 */
	private AttachProvider(Object delegate) {
		super(delegate);
	}

}
