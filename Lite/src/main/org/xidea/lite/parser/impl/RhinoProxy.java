package org.xidea.lite.parser.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.mozilla.javascript.Callable;
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
public class RhinoProxy extends JSProxy implements ErrorReporter {
	private static Context context = Context.enter();
	private static Scriptable scope = ScriptRuntime.getGlobal(context);

	private Callable fn = (Callable) eval("(function(){"
			+ "var thiz = this[0];" + "var name = this[1];"
			+ "print(uneval(thiz[name]));" +
					"return thiz[name].apply(thiz,arguments);\n"
			+ "})");

	public Object eval(String source) {
		return context.evaluateString(scope, source, "<file>", 1, null);
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

	@Override
	public <T> T getInterface(final Object thiz, Class<T> clasz) {
		return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(),
				new Class[] { clasz }, new InvocationHandler() {
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						// System.out.println(proxy+"!!!!!!!!!!");
						return invokeTypedMethod(thiz, method.getName(), method.getReturnType(),args);
					}

				});
	}

	@Override
	public Object invokeMethod(Object thiz, String name, Object... args) {
		return invokeTypedMethod(thiz, name, Object.class, args);
	}
	private Object invokeTypedMethod(Object thiz, String name, Class<? extends Object> type,Object[] args) {
		if (args == null) {
			args = new Object[0];
		}
		// return null;
		Object result = Context.call(null, fn, scope, Context.toObject(new Object[] {
				thiz, name }, scope), args);
		return Context.jsToJava(result, type);
	}
}
