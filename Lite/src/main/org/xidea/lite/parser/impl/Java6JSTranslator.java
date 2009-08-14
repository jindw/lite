package org.xidea.lite.parser.impl;


import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sun.org.mozilla.javascript.internal.CompilerEnvirons;
import sun.org.mozilla.javascript.internal.Decompiler;
import sun.org.mozilla.javascript.internal.ErrorReporter;
import sun.org.mozilla.javascript.internal.EvaluatorException;
import sun.org.mozilla.javascript.internal.Parser;
import sun.org.mozilla.javascript.internal.UintMap;

@SuppressWarnings("unchecked")
public class Java6JSTranslator extends JSTranslator implements ErrorReporter {
	private static Log log = LogFactory.getLog(CoreXMLNodeParser.class);
	private static ScriptEngine jsengine;
	static {
		jsengine = new ScriptEngineManager().getEngineByExtension("js");
		if (jsengine == null) {
			throw new RuntimeException("Java 6 找不到可用的 jsengine");
		}
	}

	public Object invokeFunction(String name, Object... args)
			throws ScriptException, NoSuchMethodException {
		return ((Invocable)jsengine).invokeFunction(name, args);
	}

	public Object invokeMethod(Object thiz, String name, Object... args)
			throws ScriptException, NoSuchMethodException {
		return ((Invocable)jsengine).invokeMethod(thiz, name, args);
	}

	@Override
	protected Object eval(String source) {
		try {
			return jsengine.eval(source);
		} catch (ScriptException e) {
			log.error(e);
			throw new RuntimeException(e);
		}
	}

	public EvaluatorException runtimeError(String message, String sourceName,
			int line, String lineSource, int lineOffset) {
		return new EvaluatorException(message);
	}

	public String compress(String source) {
		// , int linebreakpos
		CompilerEnvirons penv = new CompilerEnvirons();
		penv.setReservedKeywordAsIdentifier(true);
		Parser parser = new Parser(penv, this);
		parser.parse(source, "<script>", 1);
		String encoded = parser.getEncodedSource();
		UintMap setting = new UintMap();
		setting.put(Decompiler.INDENT_GAP_PROP, 0);
		setting.put(Decompiler.INDENT_GAP_PROP, 0);
		setting.put(Decompiler.TO_SOURCE_FLAG, 0);
		String result = Decompiler.decompile(encoded, 0, setting);
		// return result.replaceAll("(\\}[\r\n])|[\r\n]", "$1");
		return result.replaceAll("[\r\n]", "");
	}

}
