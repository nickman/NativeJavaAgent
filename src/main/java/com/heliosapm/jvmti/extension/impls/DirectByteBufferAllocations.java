/**
 * 
 */
package com.heliosapm.jvmti.extension.impls;

import java.nio.ByteBuffer;

import org.pmw.tinylog.Logger;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.heliosapm.jvmti.agent.NativeAgent;
import com.heliosapm.jvmti.extension.Scheduled;
import com.heliosapm.jvmti.extension.ScheduledExtension;

/**
 * @author nwhitehead
 *
 */
@Scheduled(fixedDelay=5000, initialDelay=1000)
public class DirectByteBufferAllocations extends ScheduledExtension {
	private static final String DBB_NAME = "java.nio.DirectByteBuffer";
	private static volatile Class<? extends ByteBuffer> clazz = null;
	private final long[] instanceCount = new long[]{0};
	private final long[] totalAllocated = new long[]{0};

	private final Gauge<Long>  instanceCountGauge = new Gauge<Long>() {
		@Override
		public Long getValue() {			
			return instanceCount[0];
		}
	};
	private final Gauge<Long>  totalAllocatedGauge = new Gauge<Long>() {
		@Override
		public Long getValue() {			
			return totalAllocated[0];
		}
	};
	
	/**
	 * @param metricRegistry
	 * @param nativeAgent
	 */
	public DirectByteBufferAllocations(MetricRegistry metricRegistry, NativeAgent nativeAgent) {
		super(metricRegistry, nativeAgent);
		metricRegistry.register(MetricRegistry.name(getClass(), "instance.count"), instanceCountGauge);
		metricRegistry.register(MetricRegistry.name(getClass(), "instance.allocation"), totalAllocatedGauge);
	}

	/**
	 * 
	 * @see com.heliosapm.jvmti.extension.ScheduledExtension#doRun()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void doRun() throws Exception {
		if(clazz==null) {
			try {
				clazz = (Class<? extends ByteBuffer>) Class.forName(DBB_NAME);
			} catch (Exception ex) {
				Logger.error("Failed to load class [{}]. Stopping scheduled execution.", DBB_NAME, ex);
				if(scheduleHandle != null) {
					scheduleHandle.cancel(true);
				}
				return;
			}
		}
		final long[] iCount = new long[]{0};
		final long[] tAllocated = new long[]{0};
		nativeAgent.instancesOf(clazz, Integer.MAX_VALUE, buff -> {
			iCount[0]++;
			tAllocated[0] += buff.capacity();			
		});
		instanceCount[0] = iCount[0];
		totalAllocated[0] = tAllocated[0];		
	}

}
