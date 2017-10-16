/**
 * 
 */
package com.heliosapm.jvmti.jmx;

import org.pmw.tinylog.Logger;

import com.heliosapm.jvmti.agent.NativeAgent;
import com.heliosapm.utils.jmx.JMXHelper;

/**
 * @author nwhitehead
 *
 */
public class NativeOps implements NativeOpsMXBean {
	final NativeAgent nativeAgent;
	
	public NativeOps() {
		nativeAgent = NativeAgent.getInstance();
		JMXHelper.registerMBean(JMXHelper.objectName("com.heliosapm.jvmti.jmx:service=NativeOps"), this);
		Logger.info("NativeOps installed");
	}
	
	@Override
	public int getInstanceCountOf(String exactTypeName) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int getInstanceCountOfAny(String anyTypeName) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	protected Class<?> getClassForName(String name) {
		try {
			return null; // TODO
		} catch (Exception ex) {
			Logger.error("Failed to find class: {}", name, ex);
			throw new RuntimeException("Failed to find class: " + name, ex);
		}
	}
}
