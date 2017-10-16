/**
 * 
 */
package com.heliosapm.jvmti.extension;

import java.util.concurrent.ScheduledFuture;

import org.pmw.tinylog.Logger;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.heliosapm.jvmti.agent.NativeAgent;

/**
 * @author nwhitehead
 *
 */
public abstract class ScheduledExtension implements Runnable {
	
	protected ScheduledFuture<?> scheduleHandle = null;
	protected final MetricRegistry metricRegistry;
	protected final NativeAgent nativeAgent;
	protected final Timer runTimer;
	protected final Counter runErrors;
	protected final long fixedDelay;
	protected final long initialDelay;
	
	
	
	public abstract void doRun() throws Exception;
	
	protected ScheduledExtension(final MetricRegistry metricRegistry, final NativeAgent nativeAgent) {
		this.metricRegistry = metricRegistry;
		this.nativeAgent = nativeAgent;
		runTimer = metricRegistry.timer(MetricRegistry.name(getClass(), "timer"));
		runErrors = metricRegistry.counter(MetricRegistry.name(getClass(), "errors"));
		Scheduled scheduled = getClass().getAnnotation(Scheduled.class);
		if(scheduled!=null && scheduled.fixedDelay()!=-1) {
			fixedDelay = scheduled.fixedDelay();
			initialDelay = scheduled.initialDelay();
		} else {
			fixedDelay = -1;
			initialDelay = -1;			
		}
	}
	
	public final void run() {
		Context ctx = runTimer.time();
		try {
			doRun();
			ctx.close();
		} catch (Exception ex) {
			runErrors.inc();
			Logger.warn("ScheduledExtension execution failure", ex);
			ex.printStackTrace(System.err);
		}
	}

	/**
	 * @return the fixedDelay
	 */
	public long getFixedDelay() {
		return fixedDelay;
	}

	/**
	 * @return the initialDelay
	 */
	public long getInitialDelay() {
		return initialDelay;
	}

	/**
	 * @return the scheduleHandle
	 */
	public ScheduledFuture<?> getScheduleHandle() {
		return scheduleHandle;
	}

	/**
	 * @param scheduleHandle the scheduleHandle to set
	 */
	public void setScheduleHandle(ScheduledFuture<?> scheduleHandle) {
		this.scheduleHandle = scheduleHandle;
	}

}
