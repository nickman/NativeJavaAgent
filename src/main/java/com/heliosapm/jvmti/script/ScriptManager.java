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
package com.heliosapm.jvmti.script;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * <p>Title: ScriptManager</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.jvmti.script.ScriptManager</code></p>
 */

public class ScriptManager {
	/** The singleton instance */
	private static volatile ScriptManager instance = null;
	/** The singleton instance ctor lock */
	private static Object lock = new Object();
	
	/** The script engine manager */
	private final ScriptEngineManager sem;
	/** A map of script engines keyed by the lower case extension */
	private final ConcurrentHashMap<String, ScriptEngine> scriptEngines = new ConcurrentHashMap<String, ScriptEngine>(16); 
	
	
	class FileTs {
		final File scriptFile;
		long timestamp = -1;
		CompiledScript cs = null;
		final ScriptEngine se;
		
		FileTs(final String fileName) {			
			scriptFile = new File(fileName);
			timestamp = scriptFile.lastModified();
			final int index = fileName.lastIndexOf('.');
			final String ext;
			if(index==-1) {
				ext = "js";
			} else {
				ext = fileName.substring(index+1);
			}
			se = scriptEngines.get(ext);
			if(se==null) throw new RuntimeException("No script engine for file [" + fileName + "]");
			compile();
		}
		
		void compile() {
			FileReader fr = null;
			try {
				fr = new FileReader(scriptFile);
				((Compilable)se).compile(fr);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			} catch (ScriptException e) {
				throw new RuntimeException(e);				
			} finally {
				try { fr.close(); } catch (Exception x) {/* No Op */}
			}
		}
	}
	
	/**
	 * Acquires the singleton ScriptManager instance
	 * @return the ScriptManager
	 */
	public static ScriptManager getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ScriptManager();
				}
			}
		}
		return instance;
	}
	
	private ScriptManager() {
		sem = new ScriptEngineManager(getClass().getClassLoader());
//		sem.getEngineFactories().stream()
//			.forEach(sef -> {
//				final ScriptEngine se = sef.getScriptEngine();
//				if(se instanceof Compilable) {
//					sef.getExtensions().stream().forEach(ext -> {
//						scriptEngines.put(ext.toLowerCase(), se);
//					});
//				}
//			});
	}
	
}
