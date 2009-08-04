package org.xidea.lite.parser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Decompiler;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.UintMap;
import org.xidea.el.json.JSONDecoder;

@SuppressWarnings("unchecked")
public class RhinoJSTranslator extends JSTranslator {
	private static Log log = LogFactory.getLog(RhinoJSTranslator.class);
	private static Context context = Context.enter();
	private static Scriptable scope = ScriptRuntime.getGlobal(context);
	protected static String DEFAULT_FEATRUES;
	static {
		ClassLoader loader = Java6JSTranslator.class.getClassLoader();
		try {
			InputStream boot = loader.getResourceAsStream("boot.js");
			if (boot != null) {
				try {
					eval(new InputStreamReader(boot, "utf-8"));
					eval("$import('org.xidea.lite:ResultTranslator')");
				} catch (Exception e) {
					log.debug("尝试JSI启动编译脚本失败", e);
				}
			}
			if (boot == null) {
				eval("var window = this;");
				InputStream compressed = loader
						.getResourceAsStream("org/xidea/lite/template.js");
				eval(new InputStreamReader(compressed, "utf-8"));
			}
			eval("var transformer = new ResultTranslator();");
			supportFeatrues = new HashSet<String>(
					(Collection) JSONDecoder
							.decode((String) eval("uneval(transformer.getSupportFeatrues())")));
		} catch (Exception e) {
			log.error("初始化Rhino JS引擎失败", e);
		}
	}

	protected static Set<String> supportFeatrues = new HashSet<String>();

	public Set<String> getSupportFeatrues() {
		return supportFeatrues;
	}

	private static Object eval(Reader in) throws IOException {
		StringWriter out = new StringWriter();
		int count;
		char[] cbuf = new char[1024];
		while ((count = in.read(cbuf)) > -1) {
			out.write(cbuf, 0, count);
		}
		return eval(out.toString());

	}

	private static Object eval(String source) {
		return context.evaluateString(scope, source, "<file>", 1, null);
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

	public String buildJS(String script) {
		String code = (String) eval(script);
		eval("function x(){" + code + "}");
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
