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

import java.util.concurrent.Callable;

import com.heliosapm.shorthand.attach.vm.VirtualMachine;

/**
 * <p>Title: VirtualMachineTask</p>
 * <p>Description: Defines a task to be run against a VirtualMachine instance</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.agent.VirtualMachineTask</code></p>
 */

public interface VirtualMachineTask<T> extends Callable<T> {
	/**
	 * Sets the VirtualMachine for the task to execute against
	 * @param vm the VirtualMachine
	 */
	public void setVirtualMachine(VirtualMachine vm);
	
	
}
