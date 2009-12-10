package org.xidea.lite.parser.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Decompiler;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.UintMap;

@SuppressWarnings("unchecked")
public class RhinoProxy extends JSProxy implements ErrorReporter {
	private Scriptable globals;

	public RhinoProxy() {
		try {
			Context context = Context.enter();
			globals = ScriptRuntime.getGlobal(context);
			initialize();
		} finally {
			Context.exit();
		}
	}

	@Override
	public <T> T wrapToJava(final Object thiz, Class<T> clasz) {
		return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(),
				new Class[] { clasz }, new InvocationHandler() {
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						// System.out.println(proxy+"!!!!!!!!!!");
						return invokeTypedMethod(thiz, method.getName(), method
								.getReturnType(), args);
					}

				});
	}

	@Override
	public Object invoke(Object thiz, String name, Object... args) {
		return invokeTypedMethod(thiz, name, Object.class, args);
	}

	private Object invokeTypedMethod(Object thiz, String name,
			Class<? extends Object> type, Object[] args) {
		if (args == null) {
			args = new Object[0];
		}
		try {
			Context cx = Context.enter();
			cx.getWrapFactory().setJavaPrimitiveWrap(primitiveToJS);
			Scriptable rhinoThiz = Context.toObject(thiz, globals);
			Function fn = (Function) ScriptableObject.getProperty(rhinoThiz,
					name);
			// return null;
			Object result = fn.call(cx, globals, rhinoThiz, args);
			if (type == Void.TYPE) {
				return null;
			} else {
				return Context.jsToJava(result, type);
			}
		} finally {
			Context.exit();
		}
	}


	public Object eval(String source, String fileName,
			Map<String, Object> varMap) {
		try {
			Context cx = Context.enter();
			cx.getWrapFactory().setJavaPrimitiveWrap(primitiveToJS);
			Scriptable localScope = globals;
			if (varMap != null) {
				localScope = cx.newObject(globals);
				for (Object key : globals.getIds()) {
					if(key instanceof String){
						String index = (String)key;
						Object value = globals.get(index, globals);
						localScope.put(index, localScope, value);
					}
				}
				for (String key : varMap.keySet()) {
					localScope.put(key, localScope, varMap.get(key));
				}
			}
			return cx.evaluateString(localScope, source, fileName, 1, null);
		} finally {
			Context.exit();
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
