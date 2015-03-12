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

/**
 * <p>Title: Agent</p>
 * <p>Description: Native interface class to expose the native-agent's functionality</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.agent.Agent</code></p>
 */

public class Agent {

	public static void main(String[] args) {	
		System.out.println("Hello World");
		int a = countInstances(Thread.class);
       	System.out.println("There are " + a + " instances of " + Thread.class);		
       	Object[] objs = getAllInstances(Thread.class, 147L, 150);
       	System.out.println("Arr Length:" + objs.length);
       	System.out.println("Objects: " + java.util.Arrays.toString(objs));
	}
	
	public static native int countInstances(Class klass);
	public static native Object[] getAllInstances(Class klass, long tag, int maxInstances);
}	
	