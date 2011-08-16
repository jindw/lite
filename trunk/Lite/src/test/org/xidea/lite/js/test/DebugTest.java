package org.xidea.lite.js.test;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.debugger.Main;
import org.mozilla.javascript.tools.debugger.ScopeProvider;
import org.mozilla.javascript.tools.shell.Global;
import org.xidea.jsi.JSIRuntime;
import org.xidea.jsi.impl.RuntimeSupport;

public class DebugTest {
	static RuntimeSupport rt = (RuntimeSupport) RuntimeSupport.create();
	public void test(){
		Object fn = rt.eval("({test:function(arg){return arg.test2()},a:1}['test'])");
		System.out.println(fn);
		rt.invoke(null,fn, this);
	}
	public void test2(){
		System.out.println(111);
		System.out.println(222);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Context.enter();
		Context context = Context.getCurrentContext();

		System.out.println(context.getDebugger());
		   Main main = new Main("Rhino JavaScript Debugger");
	        main.doBreak();
	        main.setExitAction(new IProxy(IProxy.EXIT_ACTION));

	        System.setIn(main.getIn());
	        System.setOut(main.getOut());
	        System.setErr(main.getErr());

	        Global global = org.mozilla.javascript.tools.shell.Main.getGlobal();
	        global.setIn(main.getIn());
	        global.setOut(main.getOut());
	        global.setErr(main.getErr());

	        main.attachTo(context.getFactory());

	        main.setScope(global);

	        main.pack();
	        main.setSize(600, 460);
	        main.setVisible(true);

		//tj.test();
		System.out.println(context.getDebugger());
		context.evaluateString(global, "//\nfunction test(){\njava.lang.System.out.println(123)}", "test.js", 1, null);
		Context.exit();

		//rt.invoke(fn,"test", fn);
	}
	 private static class IProxy implements Runnable, ScopeProvider
	    {
	        static final int EXIT_ACTION = 1;
	        static final int SCOPE_PROVIDER = 2;

	        private final int type;
	        Scriptable scope;

	        IProxy(int type)
	        {
	            this.type = type;
	        }

	        public static ScopeProvider newScopeProvider(Scriptable scope)
	        {
	            IProxy scopeProvider = new IProxy(SCOPE_PROVIDER);
	            scopeProvider.scope = scope;
	            return scopeProvider;
	        }

	        public void run()
	        {
	            if (type != EXIT_ACTION) Kit.codeBug();
	            System.exit(0);
	        }

	        public Scriptable getScope()
	        {
	            if (type != SCOPE_PROVIDER) Kit.codeBug();
	            if (scope == null) Kit.codeBug();
	            return scope;
	        }
	    }

}
