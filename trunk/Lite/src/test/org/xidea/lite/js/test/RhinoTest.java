package org.xidea.lite.js.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.xidea.lite.parser.impl.CoreXMLNodeParser;
import org.xidea.lite.parser.impl.Java6JSTranslator;

import com.sun.xml.internal.bind.v2.ContextFactory;



public class RhinoTest {
	private static Log log = LogFactory.getLog(CoreXMLNodeParser.class);
	private static Context context = Context.enter();
	private static Scriptable scope = ScriptRuntime.getGlobal(context);

	static {
		ClassLoader loader = Java6JSTranslator.class.getClassLoader();
		try {
			InputStream boot = loader.getResourceAsStream("boot.js");

			if (boot != null) {
				try {
					eval(new InputStreamReader(boot, "utf-8"));
					eval("$import('org.xidea.lite:buildNativeJS')");
				} catch (Exception e) {
					log.debug("尝试JSI启动编译脚本失败", e);
				}
			}
			if (boot == null) {
				InputStream compressed = loader
						.getResourceAsStream("org/xidea/lite/template.js");
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
}
