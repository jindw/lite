package org.xidea.lite.parser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.List;

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
import org.xidea.el.json.JSONEncoder;

public class RhinoJSBuilder implements JSBuilder {
	private static Log log = LogFactory.getLog(CoreXMLParser.class);
	private static Context context = Context.enter();
	private static Scriptable scope = ScriptRuntime.getGlobal(context);

	static {
		ClassLoader loader = RhinoJSBuilder.class.getClassLoader();
		try {
			InputStream uncompressedParser = loader
					.getResourceAsStream("org/xidea/lite/parser.js");
			InputStream uncompressedCompiler = loader
					.getResourceAsStream("org/xidea/lite/native-compiler.js");
			InputStream compressed = loader
					.getResourceAsStream("org/xidea/lite/template.js");
			if (uncompressedParser != null && uncompressedCompiler != null) {
				eval(new InputStreamReader(uncompressedParser, "utf-8"));

				eval(new InputStreamReader(uncompressedCompiler, "utf-8"));
			} else {
				eval(new InputStreamReader(compressed, "utf-8"));
			}
		} catch (Exception e) {
			log.error("初始化Rhino JS引擎失败", e);
		}
	}

	private static Object eval(Reader in) throws IOException {
		StringWriter out = new StringWriter();
		int count;
		char[] cbuf = new char[1024];
		while((count = in.read(cbuf))>-1){
			out.write(cbuf,0,count);
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

	public String buildJS(List<Object> liteCode,String name) {
		String source = JSONEncoder.encode(liteCode);
		String code;
		try {
			code = (String) eval("buildNativeJS(eval(" + source
					+ "))+''");
			eval("+function(){" + code + "}");
		} catch (Exception e) {
			code = "alert('生成js代码失败：'+" + JSONEncoder.encode(e.getMessage())
					+ ")";
			log.warn("生成js代码失败：", e);
		}
		return "function " + name + "(_$0,_$1,_$2){\n" + code + "\n}";
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
