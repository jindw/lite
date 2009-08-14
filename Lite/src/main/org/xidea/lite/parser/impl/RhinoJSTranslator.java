package org.xidea.lite.parser.impl;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Decompiler;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.UintMap;

@SuppressWarnings("unchecked")
public class RhinoJSTranslator extends JSTranslator implements ErrorReporter {
	private static Context context = Context.enter();
	private static Scriptable scope = ScriptRuntime.getGlobal(context);


	public Object eval(String source) {
		return context.evaluateString(scope, source, "<file>", 1, null);
	}

	public EvaluatorException runtimeError(String message, String sourceName, int line,
            String lineSource, int lineOffset) {
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
