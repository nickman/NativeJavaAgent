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
package com.heliosapm.jvmti.util;

import java.net.URL;

/**
 * <p>Title: IsolatedClassLoaderMBean</p>
 * <p>Description: JMX MBean interface for {@link IsolatedClassLoader} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.util.IsolatedClassLoaderMBean</code></p>
 */

public interface IsolatedClassLoaderMBean {
	/**
	 * Returns the URLs that comprise the classloaders isolated classpath
	 * @return an array of URLs
	 */
	public URL[] getURLs();

}
