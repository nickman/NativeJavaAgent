/**
 * 
 */
package com.heliosapm.jvmti.extension.impls.thread;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.pmw.tinylog.Logger;

import com.codahale.metrics.MetricRegistry;
import com.heliosapm.jvmti.agent.NativeAgent;
import com.heliosapm.jvmti.extension.ScheduledExtension;
import com.heliosapm.jvmti.install.JavaAgent2;
import com.heliosapm.utils.unsafe.UnsafeAdapter;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

/**
 * @author nwhitehead
 *
 */
public class ThreadPoolMonitor extends ScheduledExtension {

	private static final AtomicBoolean TPOOLS_INSTRUMENTED = new AtomicBoolean(false);

	public ThreadPoolMonitor(MetricRegistry metricRegistry, NativeAgent nativeAgent) {
		super(metricRegistry, nativeAgent);
		instrumentThreadPools();
	}
	
	@Override
	public void doRun() throws Exception {
		// TODO Auto-generated method stub
		
	}
	

	public static void beforeExecute(Thread t, Runnable r) {
		Logger.info("BEFORE EXEC [{}]: {}, {}", Thread.currentThread(), t, r);
	}
	
	public static void afterExecute(Runnable r, Throwable t) {
		Logger.info("AFTER EXEC [{}]: {}, {}", Thread.currentThread(), r, t);
	}
	
	private static void instrumentThreadPools() {
		final Instrumentation instr = JavaAgent2.INSTRUMENTATION;
		if(instr==null) return;
		if(TPOOLS_INSTRUMENTED.compareAndSet(false, true)) {
			try {
				
				ClassPool cp = new ClassPool();
				cp.appendSystemPath();
				CtClass threadCtClass = cp.get(Thread.class.getName());
				CtClass runnableCtClass = cp.get(Runnable.class.getName());
				CtClass throwableCtClass = cp.get(Runnable.class.getName());
				CtClass ME = cp.get("com.heliosapm.jvmti.extension.impls.thread.ThreadPoolMonitor");
				CtClass threadPoolCtClass = cp.get(ThreadPoolExecutor.class.getName());
				CtMethod before = threadPoolCtClass.getDeclaredMethod("beforeExecute");
				CtMethod after = threadPoolCtClass.getDeclaredMethod("afterExecute");
				before.insertBefore("{com.heliosapm.jvmti.extension.impls.thread.ThreadPoolMonitor.beforeExecute($1, $2);}");
				after.insertBefore("{com.heliosapm.jvmti.extension.impls.thread.ThreadPoolMonitor.afterExecute($1, $2);}");
				final byte[] byteCode = threadPoolCtClass.toBytecode();
				final byte[] meCode = ME.toBytecode();
				
				final String clazzName = ThreadPoolExecutor.class.getName().replace('.', '/');
				final String myClazzName = "com.heliosapm.jvmti.extension.impls.thread.ThreadPoolMonitor".replace('.', '/');
				final ProtectionDomain pd = ThreadPoolExecutor.class.getProtectionDomain();
				final ClassFileTransformer transformer = new ClassFileTransformer() {
					@Override
					public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
							ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
						if(clazzName.equals(className)) {
							UnsafeAdapter.defineClass("com.heliosapm.jvmti.extension.impls.thread.ThreadPoolMonitor", meCode, 0, meCode.length, loader, pd);
							
							return byteCode;
						}
						return classfileBuffer;
					}
				};
				try {
					instr.addTransformer(transformer, true);
					instr.retransformClasses(ThreadPoolExecutor.class);
					Logger.info("Instrumented ThreadPools");
				} finally {
					instr.removeTransformer(transformer);
				}
				
			} catch (Exception ex) {
				Logger.error("Failed to instrument ThreadPools", ex);
				ex.printStackTrace(System.err);
				TPOOLS_INSTRUMENTED.set(false);
			}
		}
		
	}

}
