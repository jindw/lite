package org.xidea.lite.test.runner;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.debug.Debugger;
import org.mozilla.javascript.tools.debugger.Dim;
import org.mozilla.javascript.tools.debugger.Main;
import org.mozilla.javascript.tools.debugger.ScopeProvider;
import org.mozilla.javascript.tools.shell.Global;
import org.xidea.jsi.JSIRuntime;
import org.xidea.jsi.impl.RuntimeSupport;

public class DebugTest {
	static RuntimeSupport rt = (RuntimeSupport) RuntimeSupport.create();

	public void test() {
		Object fn = rt
				.eval("({test:function(arg){return arg.test2()},a:1}['test'])");
		System.out.println(fn);
		rt.invoke(null, fn, this);
	}

	public void test2() {
		System.out.println(111);
		System.out.println(222);
	}

	private static Main init(ContextFactory factory,Global global) {
		Main main = new Main("Test");
		main.doBreak();
		main.attachTo(factory);
		Scriptable scope = (Scriptable) global;
		main.setScope(scope);
		main.pack();
		main.setSize(600, 460);
		main.setVisible(true);
		return main;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// org.mozilla.javascript.tools.shell.Main.getGlobal();
		// Main main = new Main("Rhino JavaScript Debugger");
		// main.doBreak();
		// main.setExitAction(new IProxy(IProxy.EXIT_ACTION));

		// main.attachTo(context.getFactory());
		//
		// main.setScope(global);
		//
		// main.pack();
		// main.setSize(600, 460);
		// main.setVisible(true);
		// context.setDebugger(new Debugger() {
		//				
		// public void handleCompilationDone(Context cx, DebuggableScript
		// fnOrScript,
		// String source) {
		//					
		// }
		//				
		// public DebugFrame getFrame(Context cx, DebuggableScript fnOrScript) {
		// return null;
		// }
		// }, null);
		//
		// //tj.test();

		final ContextFactory factory = ContextFactory.getGlobal();
		final Global global = new Global();
		init(factory, global);
		factory.call(new ContextAction() {
			public Object run(Context cx) {
				global.init(cx);
				Object test = cx
						.evaluateString(
								global,
								"//\nfunction test(a,b){\njava.lang.System.out.println(a+b)\n};test",
								"test.js", 1, null);
				Context.call(factory, (Callable)test, global, null, new Object[]{1,2});
				return null;
			}

		});
		// rt.invoke(fn,"test", fn);
	}


}
