package org.xidea.lite.parser.impl.test;


import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.jsi.JSIRuntime;
import org.xidea.jsi.impl.RuntimeSupport;
import org.xidea.lite.parse.NodeParser;
import org.xidea.lite.parse.TextParser;

public class JSProxyTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testJava6Proxy() throws Exception{
		test(Class.forName("org.xidea.jsi.impl.Java6Impl"));
	}
	@Test
	public void testRhinoProxy() throws Exception{
		test(Class.forName("org.xidea.jsi.impl.RhinoImpl"));
	}

	private void test(Class<? extends Object> c)throws Exception{
		Method method = c.getMethod("create", Boolean.TYPE);
		method.setAccessible(true);
		JSIRuntime p = (JSIRuntime) method.invoke(null, true);
		p.eval("var a = 1;");
		method = RuntimeSupport.class.getDeclaredMethod("initialize");
		method.setAccessible(true);
		method.invoke(p);
		Map<String, Object> varMap=new HashMap<String, Object>();
		varMap.put("b", 2.0);
		
		Assert.assertEquals("4",p.eval(null,"a = a+1;String(a +b)",".",varMap));
		Assert.assertEquals("2",p.eval(null,"String(a)",".",varMap));

		createTextParser(p,p.eval("(function(){})"));
		createNodeParser(p,p.eval("(function(){})"));
		//p.eval("this.a=5",".",varMap);
		//Assert.assertEquals("5",p.eval("String(a)","."));
	}
	public TextParser createTextParser(JSIRuntime proxy,Object o){
		HashMap<String, Object> varMap = new HashMap<String, Object>();
		varMap.put("impl", o);
		proxy.eval(null,
				"if(impl instanceof Function){impl.parse=impl,impl.findStart=impl}" +
				"if(!impl.getPriority) {" +
				"impl.getPriority=function(){" +
				"return impl.priority == null? 1 : impl.priority;" +
				"}};",this.getClass().toString(),varMap);
		return proxy.wrapToJava(o, TextParser.class);
	}
	@SuppressWarnings("unchecked")
	public NodeParser<? extends Object> createNodeParser(JSIRuntime proxy,Object o){
		HashMap<String, Object> varMap = new HashMap<String, Object>();
		varMap.put("impl", o);
		proxy.eval(null,
				"if(impl instanceof Function){impl.parse=impl};"
				,this.getClass().toString(),varMap);
		return proxy.wrapToJava(o, NodeParser.class);
	}
}
