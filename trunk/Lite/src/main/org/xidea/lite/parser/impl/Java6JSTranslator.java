package org.xidea.lite.parser.impl;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONDecoder;

import sun.org.mozilla.javascript.internal.CompilerEnvirons;
import sun.org.mozilla.javascript.internal.Decompiler;
import sun.org.mozilla.javascript.internal.ErrorReporter;
import sun.org.mozilla.javascript.internal.EvaluatorException;
import sun.org.mozilla.javascript.internal.Parser;
import sun.org.mozilla.javascript.internal.UintMap;

@SuppressWarnings("unchecked")
public class Java6JSTranslator extends JSTranslator {
	private static Log log = LogFactory.getLog(CoreXMLParser.class);
	private static ScriptEngine jsengine;

	static {
		jsengine = new ScriptEngineManager().getEngineByExtension("js");
		ClassLoader loader = Java6JSTranslator.class.getClassLoader();
		try {
			InputStream boot = loader.getResourceAsStream("boot.js");
			if (boot != null) {
				try {
					jsengine.eval(new InputStreamReader(boot, "utf-8"));
					jsengine
							.eval("$import('org.xidea.lite:ResultTranslator')");
				} catch (Exception e) {
					log.debug("尝试JSI启动编译脚本失败", e);
				}
			}
			if (boot == null) {
				jsengine.eval(new InputStreamReader(loader
						.getResourceAsStream("org/xidea/lite/template.js"),
						"utf-8"));
			}
			jsengine.eval("var transformer = new ResultTranslator();");
			supportFeatrues = new HashSet<String>((Collection) JSONDecoder
					.decode((String) jsengine
							.eval("uneval(transformer.getSupportFeatrues())")));
		} catch (Exception e) {
			log.error("初始化Java6 JS引擎失败", e);
			throw new RuntimeException(e);
		}

	}
	protected static Set<String> supportFeatrues = new HashSet<String>();

	public Set<String> getSupportFeatrues() {
		return supportFeatrues;
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

	public String buildJS(String script) throws Exception {
		String code = (String) jsengine.eval(script);
		jsengine.eval("+function(){" + code + "}");
		return code;
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
