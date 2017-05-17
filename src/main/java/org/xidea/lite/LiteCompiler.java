package org.xidea.lite;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;

public class LiteCompiler {
	private final ScriptEngine engine;
	private File root;

	public LiteCompiler(File root) throws ScriptException,
			FileNotFoundException {
		this.root = root.getAbsoluteFile();
		engine = new ScriptEngineManager().getEngineByExtension("js");
		//System.out.println(engine.getClass());

		try {
			engine.put("__java_engine",engine);
			InputStream packed = LiteCompiler.class.getResourceAsStream("java-packed.js");
			if(packed != null) {
				engine.eval(new InputStreamReader(packed));
			}else {
				InputStream proxy = LiteCompiler.class.getResourceAsStream("java-proxy.js");
				if (proxy != null) {
					engine.eval(new InputStreamReader(proxy));
				}
			}
			engine.eval("var root = "
					+ JSONEncoder.encode(this.root.getAbsolutePath()) + ";");
			engine.eval("var compilerModule = require('lite/src/main/js/compiler')");
			Boolean available = (Boolean) engine
					.eval("!!(compilerModule && compilerModule.LiteCompiler)");
			if (available != null && available) {
				engine.eval("var liteCompiler = new compilerModule.LiteCompiler(root)");
				engine.eval("liteCompiler.translator = null;");
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		throw new FileNotFoundException(
				"\n\nlite module not fount !!"
						+ "\nplease execute cmd 'npm install lite' on parent path of the workdir("
						+ new File("./").getAbsolutePath() + ")!!" + "\n\ncd "
						+ new File("./").getAbsoluteFile().getParent()
						+ "\nnpm install lite");
	}
	public File getRoot(){
		return root;
	}

	@SuppressWarnings("unchecked")
	public String compile(String path) throws ScriptException {
		// engine.eval("var lite = require('lite')");
		String result = engine.eval(
				"JSON.stringify(liteCompiler.compile("
						+ JSONEncoder.encode(path) + "))").toString();
		Map<String, Object> json = JSONDecoder.decode(result);
		Map<String, Object> config = (Map<String, Object>) json.get("config");
		List<String> resources = (List<String>) json.get("resources");
		List<Object> litecode = (List<Object>) json.get("litecode");
		return JSONEncoder.encode(new Object[] { resources, litecode, config });
	}

	public void log(Object args) {
		System.out.println(args);
	}
}
