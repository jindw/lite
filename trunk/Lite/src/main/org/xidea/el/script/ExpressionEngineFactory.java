package org.xidea.el.script;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.script.ScriptEngine;

import com.sun.script.util.ScriptEngineFactoryBase;

public class ExpressionEngineFactory extends ScriptEngineFactoryBase{
	static List<String> NAMES = Arrays.asList("JSEL","jsel","JSON");
	static List<String> EXTS = Arrays.asList(".JSEL",".jsel",".json");
	static List<String> MIME_TYPES = Arrays.asList("text/jsel","application/jsel");
	static HashMap<String, String> ps = new HashMap<String, String>();
	static{
	    ps.put(ScriptEngine.ENGINE, "JSEL");
	    ps.put(ScriptEngine.ENGINE_VERSION, "2.0");
	    ps.put(ScriptEngine.NAME, "JSEL");
	    ps.put(ScriptEngine.LANGUAGE, "JSEL");
	    ps.put(ScriptEngine.LANGUAGE_VERSION, "1.0");
	}
	public List<String> getExtensions() {
		return EXTS;
	}

	public String getMethodCallSyntax(String obj, String m, String... args) {
		return obj+"."+m+'('+join(args,",")+')';
	}

	private String join(String[] args, String sep) {
		StringBuilder buf = new StringBuilder();
		for(String arg:args){
			if(buf.length()>0){
				buf.append(sep);
			}
			buf.append(arg);
		}
		return buf.toString();
	}

	public List<String> getMimeTypes() {
		return MIME_TYPES;
	}

	public List<String> getNames() {
		return NAMES;
	}

	public String getOutputStatement(String toDisplay) {
		return toDisplay;
	}

	public Object getParameter(String key) {
		return ps.get(key);
	}

	public String getProgram(String... statements) {
		return join(statements,";");
	}
	public ScriptEngine getScriptEngine() {
		return new ExpressionEngine(this);
	}
}