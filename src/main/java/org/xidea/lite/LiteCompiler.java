package org.xidea.lite;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;


public class LiteCompiler {
	private final ScriptEngine engine;
	private File root;

	public LiteCompiler(File root) throws ScriptException{
		this.root = root.getAbsoluteFile();
		engine = new ScriptEngineManager().getEngineByExtension("js");
		engine.eval(new InputStreamReader(LiteCompiler.class.getResourceAsStream("./js-java-proxy.js")));
		engine.eval("var root = "+JSONEncoder.encode(this.root.getAbsolutePath())+";");
		engine.eval("var LiteCompiler = require('lite/compiler').LiteCompiler");
		engine.eval("var liteCompiler = new LiteCompiler(root)");
		engine.eval("liteCompiler.translator = null;");
	}
	public String compile(String path) throws ScriptException{
		//engine.eval("var lite = require('lite')");
		String result = engine.eval("JSON.stringify(liteCompiler.compile("+JSONEncoder.encode(path)+"))").toString();
		Map<String,Object> json = JSONDecoder.decode(result);
		Map<String,Object> config = (Map<String,Object>)json.get("config");
		List<String> resources = (List<String>)json.get("resources");
		List<Object> litecode = (List<Object>)json.get("litecode");
		return JSONEncoder.encode(new Object[]{resources,litecode,config});
	}
	

	public static void main(String[] args) throws ScriptException, IOException {
		File root = new File("./");
		File cached = new File("./.litecode");
		LiteCompiler compiler = new LiteCompiler(root);
		HotLiteEngine engine = new HotLiteEngine(root, cached);
		String path = "/doc/guide/index.xhtml";
		Object context = new HashMap();
		long t1 = System.currentTimeMillis();
		StringWriter out = new StringWriter();
		engine.render(path, context, out);
		String result = out.toString();
		long t2 = System.currentTimeMillis();
		out = new StringWriter();
		engine.render(path, context, out);
		result = out.toString();
		long t3 = System.currentTimeMillis();
		String result3 = compiler.compile("/doc/guide/index.xhtml");
		long t4 = System.currentTimeMillis();
		String result4 = compiler.compile("/doc/guide/index.xhtml");
		long t5 = System.currentTimeMillis();
		System.out.println(result);
		System.out.println(t5-t4);
		System.out.println(t3-t4);
		System.out.println(t3-t2);
		System.out.println(t2-t1);
	}

	public void log(Object args){
		System.out.println(args);
	}
}
