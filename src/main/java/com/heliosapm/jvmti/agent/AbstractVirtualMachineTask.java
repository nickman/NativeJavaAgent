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

import com.heliosapm.shorthand.attach.vm.VirtualMachine;

/**
 * <p>Title: AbstractVirtualMachineTask</p>
 * <p>Description: An empty call virtual machine task for override</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.agent.AbstractVirtualMachineTask</code></p>
 */

public abstract class AbstractVirtualMachineTask<T> implements VirtualMachineTask<T> {
	/** The virtual machine instance */
	public VirtualMachine vm = null;

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.jvmti.agent.VirtualMachineTask#setVirtualMachine(com.heliosapm.shorthand.attach.vm.VirtualMachine)
	 */
	@Override
	public void setVirtualMachine(final VirtualMachine vm) {
		this.vm = vm;
	}

}
