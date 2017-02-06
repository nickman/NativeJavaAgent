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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * <p>Title: VirtualMachine</p>
 * <p>Description: Wrapper class for Attach API's {@link com.sun.tools.attach.VirtualMachine}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.attach.vm.VirtualMachine</code></p>
 */
public class VirtualMachine extends BaseWrappedClass {
	/** A map of virtual machine keyed by their system identity hash codes */
	private static final Map<Integer, VirtualMachine> vmInstances = new ConcurrentHashMap<Integer, VirtualMachine>();
	/** The JMXServiceURL of this VirtualMachine */
	private volatile JMXServiceURL jmxServiceURL = null;
	/** The agent property representing the JMXServiceURL of the management agent */
	public static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";
	/** The system property representing the file separator */
	public static final String FILE_SEP = "file.separator";
	/** The system property representing the JMX Remote Port */
	public static final String JMX_PORT = "com.sun.management.jmxremote.port";	
	/** The JMX management agent jar name */
	public static final String JMX_AGENT = "management-agent.jar";
	/** The system property representing the java home */
	public static final String JAVA_HOME = "java.home";
	
	/**
	 * Retrieves the VirtualMachine wrapper class for the passed VirtualMachine delegate 
	 * @param delegate the VirtualMachine delegate
	 * @return the VirtualMachine 
	 */
	public static VirtualMachine getInstance(Object delegate) {
		if(delegate==null) throw new IllegalArgumentException("The passed delegate was null", new Throwable());
		if(!VirtualMachineBootstrap.getInstance().isInstanceOf(delegate, VirtualMachineBootstrap.VM_CLASS)) {
			throw new IllegalArgumentException("The passed delegate of type [" + delegate.getClass().getName() + "] is not an instance of [" + VirtualMachineBootstrap.VM_CLASS + "]", new Throwable());
		}
		int key = System.identityHashCode(delegate);
		VirtualMachine vm = vmInstances.get(key);
		if(vm==null) {
			synchronized(vmInstances) {
				vm = vmInstances.get(key);
				if(vm==null) {
					vm = new VirtualMachine(delegate);
					vmInstances.put(key, vm);
				}
			}
		}
		return vm;
	}
	
	static {
		VirtualMachineBootstrap.getInstance();
	}

	/**
	 * Creates a new VirtualMachine wrapper instance 
	 * @param delegate The delegate
	 */
	VirtualMachine(Object delegate) {
		super(delegate);
	}
	
	/**
	 *  Tests this VirtualMachine for equality with another object.
	 * @param obj The object to compare to
	 * @return true if, and only if, the given object is a VirtualMachine that is equal to this VirtualMachine.
	 */
	@Override
	public boolean equals(Object obj){
		if(obj==null) return false;
		return delegate.equals(obj);		
	}
	
	/**
	 * Returns the string representation of the VirtualMachine. 
	 * @return the string representation of the VirtualMachine. 
	 */
	@Override
	public String toString(){
		return delegate.toString();
	}
	
	/**
	 * Returns a hash-code value for this VirtualMachine. The hash code is based upon the VirtualMachine's components, and satifies the general contract of the Object.hashCode method. 
	 * @return A hash-code value for this virtual machine
	 */
	@Override
	public int hashCode(){
		return delegate.hashCode();
	}
	
	/**
	 * Returns the provider that created this virtual machine. 
	 * @return The provider that created this virtual machine.
	 */
	public AttachProvider provider(){		
		try {			
			pushCl();
			return AttachProvider.getInstance(
					invoke(delegate, null, "provider")
			);			
		} finally {
			popCl();
		}
	}	
	
	/**
	 * Return a list of Java virtual machines. 
	 * @return The list of virtual machine descriptors.
	 */
	public static List<VirtualMachineDescriptor> list(){
		List<VirtualMachineDescriptor> list = new ArrayList<VirtualMachineDescriptor>();
		try {			
			pushCl();
			List<?> vmdDelegates = (List<?>)invoke(null, VirtualMachineBootstrap.VM_CLASS, "list");
			for(Object del: vmdDelegates) {
				list.add(VirtualMachineDescriptor.getInstance(del));
			}
			return list;			
		} finally {
			popCl();
		}		
	}
	
	/**
	 * Returns the identifier for this Java virtual machine.
	 * @return The identifier for this Java virtual machine.
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
	 * Attaches to a Java virtual machine. 
	 * @param vmd The virtual machine descriptor. 
	 * @return A VirtualMachine representing the target VM. 
	 */
	public static VirtualMachine attach(VirtualMachineDescriptor vmd) {
		if(vmd==null) throw new IllegalArgumentException("The passed VirtualMachineDescriptor was null", new Throwable());
		try {
			pushCl();
			Object vmDelegate = invoke(null, VirtualMachineBootstrap.VM_CLASS, "attachV", vmd.delegate);
			return new VirtualMachine(vmDelegate);
		} catch (Exception e) {
			throw new RuntimeException("Failed to attach to VirtualMachine [" + vmd.toString() + "]", e);
		} finally {
			popCl();
		}
	}	
	
	/**
	 * Attaches to a Java virtual machine. 
	 * @param id The abstract identifier that identifies the Java virtual machine. 
	 * @return A VirtualMachine representing the target VM. 
	 */
	public static VirtualMachine attach(String id) {
		if(id==null) throw new IllegalArgumentException("The passed VirtualMachine id was null", new Throwable());
		try {
			pushCl();
			Object vmDelegate = invoke(null, VirtualMachineBootstrap.VM_CLASS, "attachS", id);
			return new VirtualMachine(vmDelegate);
		} catch (Exception e) {
			throw new RuntimeException("Failed to attach to VirtualMachine [" + id + "]", e);
		} finally {
			popCl();
		}
	}		
	
	/**
	 * Detach from the virtual machine. 
	 */
	public void detach() {
		try {			
			pushCl();
			invoke(delegate, null, "detach");
		} finally {
			popCl();
		}				
	}	
	
	/**
	 * Returns the current agent properties in the target virtual machine. 
	 * @return The agent properties 
	 */
	public Properties getAgentProperties() {
		try {			
			pushCl();
			return (Properties)invoke(delegate, null, "getAgentProperties");
		} finally {
			popCl();
		}						
	}
	
	/**
	 * Returns the current system properties in the target virtual machine. 
	 * @return The system properties 
	 */
	public Properties getSystemProperties() {
		try {			
			pushCl();
			return (Properties)invoke(delegate, null, "getSystemProperties");
		} finally {
			popCl();
		}				
	}	
	
	/**
	 * Loads an agent.  
	 * @param agent Path to the JAR file containing the agent.  
	 */
	public void loadAgent(String agent) {
		try {			
			pushCl();
			invoke(delegate, null, "loadAgentS", agent);
		} finally {
			popCl();
		}				
	}
	
	/**
	 * Loads an agent.  
	 * @param agent Path to the JAR file containing the agent.  
	 * @param options The options to provide to the agent's agentmain method (can be null). 
	 */
	public void loadAgent(String agent, String options) {
		try {			
			pushCl();
			invoke(delegate, null, "loadAgentSS", agent, options);
		} finally {
			popCl();
		}				
	}		
	
	/**
	 * Loads an agent library. 
	 * @param agentLibrary The name of the agent library.   
	 */
	public void loadAgentLibrary(String agentLibrary) {
		try {			
			pushCl();
			invoke(delegate, null, "loadAgentLibraryS", agentLibrary);
		} finally {
			popCl();
		}				
	}	
	
	/**
	 * Loads an agent library. 
	 * @param agentLibrary The name of the agent library.   
	 * @param options The options to provide to the Agent_OnAttach function (can be null). 
	 */
	public void loadAgentLibrary(String agentLibrary, String options) {
		try {			
			pushCl();
			invoke(delegate, null, "loadAgentLibrarySS", agentLibrary, options);
		} finally {
			popCl();
		}				
	}	
	
	/**
	 * Load a native agent library by full pathname.  
	 * @param agentPath The full path to the agent library. 
	 */
	public void loadAgentPath(String agentPath) {
		try {			
			pushCl();
			invoke(delegate, null, "loadAgentPathS", agentPath);
		} finally {
			popCl();
		}				
	}
	
	/**
	 * Load a native agent library by full pathname.  
	 * @param agentPath The full path to the agent library. 
	 * @param options The options to provide to the Agent_OnAttach function (can be null). 
	 */
	public void loadAgentPath(String agentPath, String options) {
		try {			
			pushCl();
			invoke(delegate, null, "loadAgentPathSS", agentPath, options);
		} finally {
			popCl();
		}				
	}
	
	/**
	 * Returns a {@link MBeanServerConnection} to this VM instance
	 * @return a {@link MBeanServerConnection} to this VM instance
	 */
	public MBeanServerConnection getMBeanServerConnection() {		
		try {			
			return getJMXConnector().getMBeanServerConnection();
		} catch (Exception e) {
			throw new RuntimeException("Failed to acquire MBeanServerConnection from VirtualMachine [" + id() + "]", e);
		}
	}
	
	
	/**
	 * Returns a {@link JMXConnector} to this VM instance
	 * @return a {@link JMXConnector} to this VM instance
	 */
	public JMXConnector getJMXConnector() {		
		try {
			JMXServiceURL serviceURL = getJMXServiceURL();
			return JMXConnectorFactory.connect(serviceURL);
		} catch (Exception e) {
			throw new RuntimeException("Failed to acquire JMXConnector from VirtualMachine [" + id() + "]", e);
		}
	}
	
	/**
	 * Returns a {@link JMXServiceURL} to connect to this VM instance
	 * @return a {@link JMXServiceURL} to connect to this VM instance
	 * TODO: We need to allow this using authentication.
	 */
	public JMXServiceURL getJMXServiceURL() {
		if(jmxServiceURL==null) {
			synchronized(this) {
				if(jmxServiceURL==null) {
					try {
						String connAddr = getAgentProperties().getProperty(CONNECTOR_ADDRESS);
						if(connAddr==null) {
							Properties sysProps = getSystemProperties();
							String fileSep = sysProps.getProperty(FILE_SEP, File.separator);
							String javaHome = sysProps.getProperty(JAVA_HOME);
							String agentPath = String.format("%s%slib%s%s", javaHome, fileSep, fileSep, JMX_AGENT);
							loadAgent(agentPath);
							//, JMX_PORT + "=" + FreePortFinder.getNextFreePort() + ",com.sun.management.jmxremote.authenticate=false");
							connAddr = getAgentProperties().getProperty(CONNECTOR_ADDRESS);
						}
						if(connAddr==null) throw new RuntimeException("Failed to acquire JMXServiceURL for MBeanServerConnection to VirtualMachine [" + id() + "]", new Throwable());
						jmxServiceURL =  new JMXServiceURL(connAddr);			
					} catch (Exception e) {
						throw new RuntimeException("Failed to acquire JMXServiceURL from VirtualMachine [" + id() + "]", e);
					}
					
				}
			}
		}
		return jmxServiceURL;
	}

}

