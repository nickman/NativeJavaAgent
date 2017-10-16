package com.heliosapm.jvmti.extension;

public interface ExecutionSchedulerMXBean {
	
	public int getCorePoolSize();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getMaximumPoolSize()
	 */
	public int getMaximumPoolSize();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getQueue()
	 */
	public int getQueueDepth();
	
	public int getQueueAvailCap();
	

	/**
	 * 
	 * @see java.util.concurrent.ThreadPoolExecutor#purge()
	 */
	public void purge();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getPoolSize()
	 */
	public int getPoolSize();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getActiveCount()
	 */
	public int getActiveCount();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getLargestPoolSize()
	 */
	public int getLargestPoolSize();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getTaskCount()
	 */
	public long getTaskCount();

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getCompletedTaskCount()
	 */
	public long getCompletedTaskCount();
}
