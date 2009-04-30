package org.xidea.lite.parser.impl;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONEncoder;

import sun.org.mozilla.javascript.internal.CompilerEnvirons;
import sun.org.mozilla.javascript.internal.Decompiler;
import sun.org.mozilla.javascript.internal.ErrorReporter;
import sun.org.mozilla.javascript.internal.EvaluatorException;
import sun.org.mozilla.javascript.internal.Parser;
import sun.org.mozilla.javascript.internal.UintMap;

public class Java6JSBuilder implements JSBuilder {
	private static Log log = LogFactory.getLog(CoreXMLParser.class);
	private static Bindings parentScope;
	private static ScriptEngine jsengine;

	static {
		jsengine = new ScriptEngineManager().getEngineByExtension("js");

		parentScope = jsengine.createBindings();
		ClassLoader loader = Java6JSBuilder.class.getClassLoader();
		try {
			InputStream boot = loader
					.getResourceAsStream("boot.js");
			if (boot != null) {
				try {
					jsengine.eval(
							new InputStreamReader(boot, "utf-8"),
							parentScope);
					jsengine.eval("$import('org.xidea.lite:buildNativeJS')", parentScope);
				} catch (Exception e) {
					log.debug("尝试JSI启动编译脚本失败", e);
				}
			}
			if (boot == null) {
				jsengine.eval(new InputStreamReader(loader
						.getResourceAsStream("org/xidea/lite/template.js"), "utf-8"),
						parentScope);
			}
		} catch (Exception e) {
			log.error("初始化Java6 JS引擎失败", e);
			throw new RuntimeException(e);
		}

	}

	private ErrorReporter reportor = new ErrorReporter() {
		public void error(String arg0, String arg1, int arg2, String arg3,
				int arg4) {
			log.warn(arg0 + ":" + arg1 + ":" + arg3 + "@" + arg2);
		}

		public EvaluatorException runtimeError(String arg0, String arg1,
				int arg2, String arg3, int arg4) {
			return null;
		}

		public void warning(String arg0, String arg1, int arg2, String arg3,
				int arg4) {
			log.warn(arg0 + ":" + arg1 + ":" + arg3 + "@" + arg2);
		}
	};

	private CompilerEnvirons penv = new CompilerEnvirons();
	{
		penv.setReservedKeywordAsIdentifier(true);
	}

	public String buildJS(List<Object> liteCode,String name) {
		String source = JSONEncoder.encode(liteCode);
		String code;
		try {
			code = (String) jsengine.eval("buildNativeJS(eval(" + source
					+ "))+''", parentScope);
			jsengine.eval("+function(){" + code + "}");
		} catch (ScriptException e) {
			code = "alert('生成js代码失败：'+" + JSONEncoder.encode(e.getMessage())
					+ ")";
			log.warn("生成js代码失败：", e);
		}
		return "function " + name + "(){\n" + code + "\n}";
	}

	public String compress(String source) {
		// , int linebreakpos
		Parser parser = new Parser(penv, reportor);
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
