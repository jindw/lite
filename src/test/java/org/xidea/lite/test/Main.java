package org.xidea.lite.test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import javax.script.ScriptException;

import org.xidea.lite.HotLiteEngine;
import org.xidea.lite.LiteCompiler;

public class Main {
	

	public static void main(String[] args) throws ScriptException, IOException {
		File root = new File("./");
		File cached = new File("./.litecode/");
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

}
