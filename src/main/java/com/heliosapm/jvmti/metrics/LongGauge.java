/**
 * 
 */
package com.heliosapm.jvmti.metrics;

import java.util.concurrent.atomic.AtomicLong;

import com.codahale.metrics.Gauge;

/**
 * @author nwhitehead
 *
 */
public class LongGauge implements Gauge<Long> {
	final AtomicLong value = new AtomicLong();

	@Override
	public Long getValue() {		
		return value.get();
	}
	
	public void update(long value) {
		this.value.set(value);
	}
}
