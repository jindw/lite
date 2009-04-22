package org.xidea.el.js.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import org.junit.Test;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Ref;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

public class TestRhinoCallback {

	private static Context context = Context.enter();
	private static Scriptable scope = ScriptRuntime.getGlobal(context);

	private static Context context2 = Context.enter();

	private static Object eval(Reader in) throws IOException {
		StringWriter out = new StringWriter();
		int count;
		char[] cbuf = new char[1024];
		while ((count = in.read(cbuf)) > -1) {
			out.write(cbuf, 0, count);
		}
		return eval(out.toString());

	}

	private static Object eval(String source) {
		return context.evaluateString(scope, source, "<file>", 1, null);
	}

	static {
		InputStream source = TestRhinoCallback.class
				.getResourceAsStream("TestRhinoCallback.js");
		try {
			eval(new InputStreamReader(source, "utf-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void test1(int x){
		System.out.println(x);
	}
	@Test
	public void testCallback() {
		ScriptRuntime.setObjectProp(scope, "base", new TestRhinoCallback(), context);
		org.mozilla.javascript.NativeObject thisObj = (org.mozilla.javascript.NativeObject)eval("new TestRhinoCallback()");
		System.out.println(thisObj.getClass());
		Scriptable bfn = (Scriptable) ScriptRuntime.getPropFunctionAndThis(thisObj,"test1",context);
		//Ref result = ScriptRuntime.applyOrCall(isApply, context, scope, thisObj, args);
		
		Object obj = ScriptRuntime.applyOrCall(false, context, scope, bfn, new Object[]{1});
		
		//context.
	}

}
