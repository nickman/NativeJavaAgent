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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Title: VirtualMachineDescriptor</p>
 * <p>Description: Wrapper class for Attach API's {@link com.sun.tools.attach.VirtualMachineDescriptor}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.attach.vm.VirtualMachineDescriptor</code></p>
 */
public class VirtualMachineDescriptor extends BaseWrappedClass {
	/** A map of machine descriptors delegates keyed by their system identity hash codes */
	private static final Map<Integer, VirtualMachineDescriptor> vmdInstances = new ConcurrentHashMap<Integer, VirtualMachineDescriptor>();

	/**
	 * Acquires the wrapped VirtualMachineDescriptor for the passed delegate
	 * @param delegate The VirtualMachineDescriptor delegate object
	 * @return a wrapped VirtualMachineDescriptor 
	 */
	public static VirtualMachineDescriptor getInstance(Object delegate) {
		if(delegate==null) throw new IllegalArgumentException("The passed VirtualMachineDescriptor delegate was null", new Throwable());
		if(!VirtualMachineBootstrap.getInstance().isInstanceOf(delegate, VirtualMachineBootstrap.VM_DESC_CLASS)) {
			throw new IllegalArgumentException("The passed delegate of type [" + delegate.getClass().getName() + "] was not of the type [" + VirtualMachineBootstrap.VM_DESC_CLASS + "]", new Throwable());
		}		
		int id = System.identityHashCode(delegate);
		VirtualMachineDescriptor vmd = vmdInstances.get(id);
		if(vmd==null) {
			synchronized(vmdInstances) {
				vmd = vmdInstances.get(id);
				if(vmd==null) {
					vmd = new VirtualMachineDescriptor(delegate);
					vmdInstances.put(id, vmd);
				}
			}
		}
		return vmd;
	}
	
	/**
	 * Returns a list of all registered VirtualMachineDescriptors
	 * @return a list of all registered VirtualMachineDescriptors
	 */
	public static List<VirtualMachineDescriptor> getVirtualMachineDescriptors() {
		List<VirtualMachineDescriptor> results = new ArrayList<VirtualMachineDescriptor>();
		try {
			pushCl();
			for(AttachProvider ap: AttachProvider.getAttachProviders()) {
				for(VirtualMachineDescriptor vmd: ap.listVirtualMachines()) {
					results.add(vmd);
					int key = System.identityHashCode(vmd.delegate);
					if(!vmdInstances.containsKey(key)) {
						synchronized(vmdInstances) {
							if(!vmdInstances.containsKey(key)) {
								VirtualMachineDescriptor virtualMachineDescriptor = new VirtualMachineDescriptor(vmd);
								vmdInstances.put(key, virtualMachineDescriptor);
							}
						}
					}					
				}
			}
			return results;
		} catch (Exception e) {
			throw new RuntimeException("Failed to list all VirtualMachineDescriptors", e);
		} finally {
			popCl();
		}		
	}
	
	/**
	 * Return the identifier component of this descriptor. 
	 * @return The identifier component of this descriptor.
	 */
	public String id() {
		try {
			pushCl();
			return (String)invoke(delegate, null, "id");
		} finally {
			popCl();
		}
	}
	
	/**
	 * Return the display name component of this descriptor. 
	 * @return The display name component of this descriptor.
	 */
	public String displayName() {
		try {
			pushCl();
			return (String)invoke(delegate, null, "displayName");
		} finally {
			popCl();
		}
	}
	
	/**
	 * Return the AttachProvider that this descriptor references. 
	 * @return The AttachProvider that this descriptor references. 
	 */
	public AttachProvider provider() {
		try {
			pushCl();
			return AttachProvider.getInstance(invoke(delegate, null, "provider"));			
		} finally {
			popCl();
		}
	}
	

	/**
	 * Tests this VirtualMachineDescriptor for equality with another object.
	 * @param obj The object to compare to
	 * @return true if, and only if, the given object is a VirtualMachineDescriptor that is equal to this VirtualMachine.
	 */
	@Override
	public boolean equals(Object obj){
		if(obj==null) return false;
		return delegate.equals(obj);		
	}	
	
	/**
	 * Returns a hash-code value for this VirtualMachineDescriptor. The hash code is based upon the VirtualMachine's components, and satifies the general contract of the Object.hashCode method. 
	 * @return A hash-code value for this VirtualMachineDescriptor
	 */
	@Override
	public int hashCode(){
		return delegate.hashCode();
	}		
	
	/**
	 * Creates a new VirtualMachineDescriptor
	 * @param delegate The VirtualMachineDescriptor delegate object
	 */
	private VirtualMachineDescriptor(Object delegate) {
		super(delegate);
	}

	
	@Override
	public String toString() {
		return delegate.toString();
	}
}
