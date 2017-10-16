/**
 * 
 */
package com.heliosapm.jvmti.extension;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ObjectName;

import org.pmw.tinylog.Logger;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.heliosapm.jvmti.agent.NativeAgent;
import com.heliosapm.utils.jmx.JMXHelper;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;

/**
 * @author nwhitehead
 *
 */
public class ExecutionScheduler implements ExecutionSchedulerMXBean, UncaughtExceptionHandler, RejectedExecutionHandler {
	private static volatile ExecutionScheduler instance = null;
	private static final Object lock = new Object();
	public static final int CORES = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	private final DropwizardMeterRegistry promReg = new DropwizardMeterRegistry(HierarchicalNameMapper.DEFAULT, Clock.SYSTEM);
	private final MetricRegistry registry = promReg.getDropwizardRegistry();
	
	private final ScheduledThreadPoolExecutor scheduler;
	private final ThreadPoolExecutor executor;
	
	private final ObjectName objectName = JMXHelper.objectName("com.heliosapm.jvmti:service=ExecutionScheduler");
	
	public static ExecutionScheduler getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ExecutionScheduler();
				}
			}
		}
		return instance;
	}
	
	private ExecutionScheduler() {
		Metrics.globalRegistry.add(promReg);
		executor = new ThreadPoolExecutor(1, CORES, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(32), new ThreadFactory(){
			final AtomicInteger serial = new AtomicInteger(0);
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "ExecutorThread#" + serial.incrementAndGet());
				t.setDaemon(true);
				return t;
			}			
		}, this);
		scheduler = new ScheduledThreadPoolExecutor(2, new ThreadFactory(){
			final AtomicInteger serial = new AtomicInteger(0);
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "SchedulerThread#" + serial.incrementAndGet());
				t.setDaemon(true);
				return t;
			}
		}){
//			@Override
//			public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, long initialDelay, long delay, TimeUnit unit) {
//				return super.scheduleWithFixedDelay(new Runnable(){
//					public void run() {
//						executor.execute(command);
//					}
//				}, initialDelay, delay, unit);
//			}
		};
		JmxReporter.forRegistry(registry).build().start();
		JMXHelper.registerMBean(this, objectName);
		Logger.info("ExecutionScheduler Started");
	}
	
	public void schedule(String className) {
		try {
			@SuppressWarnings("unchecked")
			Class<? extends ScheduledExtension> clazz = (Class<? extends ScheduledExtension>) Class.forName(className);
			Constructor<? extends ScheduledExtension> ctor = clazz.getDeclaredConstructor(MetricRegistry.class, NativeAgent.class);
			ScheduledExtension se = ctor.newInstance(registry, NativeAgent.getInstance());
			long fixedDelay = se.getFixedDelay();
			long initialDelay = se.getInitialDelay();
			if(fixedDelay > 0) {
				ScheduledFuture<?> handle = scheduler.scheduleWithFixedDelay(new Runnable(){
					public void run() {
						executor.execute(se);
					}
				}, initialDelay, fixedDelay, TimeUnit.MILLISECONDS);
				se.setScheduleHandle(handle);
				Logger.info("Extension [{}] scheduled for repeated execution every {} ms.", className, fixedDelay);
			} else {
				Logger.info("Extension [{}] had no schedule", className);
			}
		} catch (Exception ex) {
			Logger.error("Failed to schedule extension: {}", className, ex);
			ex.printStackTrace(System.err);
		}
	}
	
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		Logger.error("Rejected Execution: {}, Qcap: {}", r, executor.getQueue().remainingCapacity());		
	}
	
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		Logger.error("Uncaught exception on thread {}", t, e);		
	}

	/**
	 * @return the registry
	 */
	public MetricRegistry getRegistry() {
		return registry;
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getCorePoolSize()
	 */
	public int getCorePoolSize() {
		return executor.getCorePoolSize();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getMaximumPoolSize()
	 */
	public int getMaximumPoolSize() {
		return executor.getMaximumPoolSize();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getQueue()
	 */
	public int getQueueDepth() {
		return executor.getQueue().size();
	}
	
	public int getQueueAvailCap() {
		return executor.getQueue().remainingCapacity();
	}
	

	/**
	 * 
	 * @see java.util.concurrent.ThreadPoolExecutor#purge()
	 */
	public void purge() {
		executor.purge();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getPoolSize()
	 */
	public int getPoolSize() {
		return executor.getPoolSize();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getActiveCount()
	 */
	public int getActiveCount() {
		return executor.getActiveCount();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getLargestPoolSize()
	 */
	public int getLargestPoolSize() {
		return executor.getLargestPoolSize();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getTaskCount()
	 */
	public long getTaskCount() {
		return executor.getTaskCount();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getCompletedTaskCount()
	 */
	public long getCompletedTaskCount() {
		return executor.getCompletedTaskCount();
	}
}
