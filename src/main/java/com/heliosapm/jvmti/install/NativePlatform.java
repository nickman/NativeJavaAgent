/**
 * 
 */
package com.heliosapm.jvmti.install;

import java.io.File;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import org.pmw.tinylog.Logger;

import com.heliosapm.utils.url.URLHelper;

/**
 * Supported native platforms
 * @author nwhitehead
 *
 */
public enum NativePlatform {
	LINUX64("LINUX", 64, "linux64", "liboifagent.so", ".so"),
	LINUX32("LINUX", 32, "linux32", "liboifagent.so", ".so"),
	WINDOWS64("WINDOWS", 64, "win64", "oifagent.dll", ".dll"),
	WINDOWS32("WINDOWS", 32, "win32", "oifagent.dll", ".dll"),
	NOIMPL(null, -1, null, null, null);
	
	private NativePlatform(final String os, final int arch, final String libDir, final String libName, final String extension) {
		this.os = os;
		this.arch =  arch;
		this.libDir = libDir;
		this.libName = libName;
		this.extension = extension;
	}
	
	public final String os;
	public final int arch;
	public final String libDir;
	public final String libName;
	public final String extension;

	private static final AtomicBoolean nativeLibLoaded = new AtomicBoolean(false);
	private static final NativePlatform[] values = values();
	
	public static boolean isNativeLibLoaded() {
		return nativeLibLoaded.get();
	}
	
	public static NativePlatform detect() {
		NativePlatform current = null;
		final String osname = System.getProperty("os.name", "").toUpperCase();
		final int archmodel = Integer.parseInt(System.getProperty("sun.arch.data.model", "-1"));
		for(NativePlatform np: values) {
			if(osname.contains(np.os) && archmodel==np.arch) current = np;
			break;
		}
		if(current==null) current = NOIMPL;
		Logger.info("NativePlatform: {}", current.name());
		if(current==NOIMPL) {
			Logger.warn("No native lib implemented for platform [{}/{}]", System.getProperty("os.name"), System.getProperty("sun.arch.data.model"));
		}		
		return current;
	}
	
	public static void load(final String lib) {
		if(!nativeLibLoaded.get()) {
			if(lib!=null) {
				try {
					File libFile = new File(lib);
					String fullPath = libFile.getAbsolutePath();
					System.load(fullPath);
					Logger.info("Loaded native library [{}]", fullPath);
					nativeLibLoaded.set(true);
				} catch (Throwable t) {
					Logger.error("Failed to load native library [{}]", lib, t);
				}
			} else {
				final NativePlatform np = detect();
				URL libUrl = null;
				if(NativePlatform.class.getProtectionDomain().getCodeSource().getLocation().toString().endsWith(".jar")) {
					try {
						libUrl = URLHelper.toURL("native/" + np.libDir + "/" + np.libName);
						File f = File.createTempFile("native-agent", np.extension);
						f.deleteOnExit();
						URLHelper.writeToFile(libUrl, f, false);
						load(f.getAbsolutePath());
					} catch (Exception ex) {
						throw new IllegalStateException("Failed t o write extracted native lib", ex);
					}
				} else {
					// We're in DEV mode
					load("./target/native/native/" + np.libDir + "/" + np.libName);
				}
			}
		}
	}
}
