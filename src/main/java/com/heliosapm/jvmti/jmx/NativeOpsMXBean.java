/**
 * 
 */
package com.heliosapm.jvmti.jmx;

/**
 * @author nwhitehead
 *
 */
public interface NativeOpsMXBean {
	/**
	 * Counts the number of heap objects of the exact passed type
	 * @param exactTypeName The name of the exact type of heap objects to count
	 * @return the number of objects found on the heap
	 */
	public int getInstanceCountOf(final String exactTypeName);
	
	/**
	 * Counts the number of heap objects of the passed type or any type inherrited from it
	 * @param anyTypeName The type name of heap objects to count
	 * @return the number of objects found on the heap
	 */
	public int getInstanceCountOfAny(final String anyTypeName);

}
