package org.jside.fileserver.test;

import java.util.List;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.junit.Test;

public class ScriptEngine {
	@Test
	public void listEngine(){
		main();
		System.out.println(new ScriptEngineManager().getEngineByExtension("js"));
	}

	public static void main(String... args) {
		System.out.print(com.sun.script.javascript.RhinoScriptEngineFactory.class);
		System.out.println(System.getProperties());
		List<ScriptEngineFactory> list = new ScriptEngineManager(ScriptEngine.class.getClassLoader()).getEngineFactories();
		System.out.println(list);

		if (new ScriptEngineManager(ScriptEngine.class.getClassLoader()).getEngineByExtension("js") == null) {
			throw new RuntimeException("Java 6 找不到可用的 jsengine");
		}
	}

}
