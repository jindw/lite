package org.xidea.lite.parser;

import java.io.InputStream;
import java.io.InputStreamReader;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONEncoder;

public class ClientJSBuilder {
	private static Log log = LogFactory.getLog(CoreXMLNodeParser.class);
	private static Bindings parentScope;
	private static ScriptEngine jsengine;
	static {
		jsengine = new ScriptEngineManager()
				.getEngineByExtension("js");
		parentScope = jsengine.createBindings();
		ClassLoader loader = ClientJSBuilder.class.getClassLoader();
		InputStream nativeParser = loader
				.getResourceAsStream("org/xidea/lite/parser/native-compiler.js");

		try {
			if (nativeParser != null) {
				jsengine.eval(new InputStreamReader(nativeParser,"utf-8"), parentScope);
			} else {
				jsengine.eval(new InputStreamReader(loader
						.getResourceAsStream("org/xidea/lite/parser.js"),
						"utf-8"), parentScope);
				jsengine.eval(new InputStreamReader(loader
						.getResourceAsStream("org/xidea/lite/native-compiler.js"),
						"utf-8"), parentScope);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String buildJS(String id, Object liteCode) {
		String source = JSONEncoder.encode(liteCode);
		String code;
		try {
			code = (String)jsengine.eval("buildNativeJS(eval("+source + "))+''",parentScope);
		} catch (ScriptException e) {
			code = "alert('生成js代码失败：'+"+JSONEncoder.encode(e.getMessage())+")";
			log.warn("生成js代码失败：",e);
		}
		return "function "+id+"(){\n"+code+"\n}";
	}
}
