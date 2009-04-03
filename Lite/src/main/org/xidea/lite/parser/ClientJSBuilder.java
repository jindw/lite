package org.xidea.lite.parser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.xidea.el.json.JSONEncoder;

public class ClientJSBuilder {
	static Bindings parentScope;
	static ScriptEngine jsengine;
	static {
		jsengine = new ScriptEngineManager()
				.getEngineByExtension("js");
		parentScope = jsengine.createBindings();
		ClassLoader loader = ClientJSBuilder.class.getClassLoader();
		InputStream nativeParser = loader
				.getResourceAsStream("org/xidea/lite/parser/native-parser.js");

		try {
			if (nativeParser != null) {
				jsengine.eval(new InputStreamReader(nativeParser,"utf-8"), parentScope);
			} else {
				jsengine.eval(new InputStreamReader(loader
						.getResourceAsStream("org/xidea/lite/parser.js"),
						"utf-8"), parentScope);
				jsengine.eval(new InputStreamReader(loader
						.getResourceAsStream("org/xidea/lite/native-parser.js"),
						"utf-8"), parentScope);

			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String buildJS(String id, Object liteCode) {
		String source = JSONEncoder.encode(liteCode);
		try {
			return (String)jsengine.eval("buildNativeJS(eval("+source + "))+''",parentScope);
		} catch (ScriptException e) {
			e.printStackTrace();
			return "function "+liteCode+"(){alert("+JSONEncoder.encode(e.getMessage())+")}";
		}
	}
}
