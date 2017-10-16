/**
 * 
 */
package com.heliosapm.jvmti.extension.impls;

import javax.management.ObjectName;

import org.pmw.tinylog.Logger;

import com.codahale.metrics.JmxAttributeGauge;
import com.codahale.metrics.MetricRegistry;
import com.heliosapm.jvmti.agent.NativeAgent;
import com.heliosapm.jvmti.extension.Scheduled;
import com.heliosapm.jvmti.extension.ScheduledExtension;
import com.heliosapm.jvmti.metrics.LongGauge;
import com.heliosapm.utils.jmx.JMXHelper;

/**
 * @author nwhitehead
 *
 */
@Scheduled(fixedDelay=5000, initialDelay=1000)
public class HotspotExtension extends ScheduledExtension {
	final boolean enabled;
	final ObjectName hotspotRuntime = JMXHelper.objectName("sun.management:type=HotspotRuntime");
	final LongGauge safepointCount = new LongGauge();
	final LongGauge safepointSyncTime = new LongGauge();
	final LongGauge totalSafepointTime = new LongGauge();
	/**
	 * @param metricRegistry
	 * @param nativeAgent
	 */
	public HotspotExtension(MetricRegistry metricRegistry, NativeAgent nativeAgent) {
		super(metricRegistry, nativeAgent);		
		enabled = JMXHelper.registerHotspotInternal();
		if(enabled) {
			metricRegistry.register(MetricRegistry.name(getClass(), "runtime.safepoint.count"), safepointCount);
			metricRegistry.register(MetricRegistry.name(getClass(), "runtime.safepoint.synctime"), safepointSyncTime);
			metricRegistry.register(MetricRegistry.name(getClass(), "runtime.safepoint.totaltime"), totalSafepointTime);
		}
	}

	/**
	 * sun.management:type=HotspotRuntime
	 * 	SafepointCount
	 * 	SafepointSyncTime
	 * 	TotalSafepointTime
	 * 
	 * @see com.heliosapm.jvmti.extension.ScheduledExtension#doRun()
	 */
	@Override
	public void doRun() throws Exception {
		if(!enabled) {
			Logger.info("HotspotExtension failed to register. Cancelling schedule");
			if(scheduleHandle!=null) {
				scheduleHandle.cancel(true);
			}
			return;			
		}
		safepointCount.update(JMXHelper.getAttribute(hotspotRuntime, "SafepointCount"));
		safepointSyncTime.update(JMXHelper.getAttribute(hotspotRuntime, "SafepointSyncTime"));
		totalSafepointTime.update(JMXHelper.getAttribute(hotspotRuntime, "TotalSafepointTime"));
		
	}

}
