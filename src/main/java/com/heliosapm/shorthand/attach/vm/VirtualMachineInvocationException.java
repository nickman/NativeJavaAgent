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

import java.io.ObjectStreamException;

/**
 * <p>Title: VirtualMachineInvocationException</p>
 * <p>Description: Exception thrown when a reflective invocation against an attached [@link VirtualMachine} fails</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.attach.vm.VirtualMachineInvocationException</code></p>
 */
public class VirtualMachineInvocationException extends RuntimeException {

	/**  */
	private static final long serialVersionUID = 8560019290540847249L;

	/**
	 * Creates a new VirtualMachineInvocationException
	 * @param message The exception message
	 * @param cause the underlying cause of the exception
	 */
	public VirtualMachineInvocationException(String message, Throwable cause) {
		super(message, cause);
	}
	
	/**
	 * Replaces this class with a standard {@link RuntimeException} when serializing
	 * since the receiving end may not have this class in its classpath
	 * @return a standard {@link RuntimeException}
	 * @throws ObjectStreamException
	 */
	Object writeReplace() throws ObjectStreamException {
		return new RuntimeException("[Converted VirtualMachineInvocationException]" + this.getMessage(), this.getCause());
	}


}
