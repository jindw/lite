package org.xidea.lite.parser.impl.test;


import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.lite.parser.impl.JSProxy;
import org.xidea.lite.parser.impl.Java6Proxy;
import org.xidea.lite.parser.impl.RhinoProxy;

public class JSProxyTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testJava6Proxy() {
		test(new Java6Proxy());
	}
	@Test
	public void testRhinoProxy() {
		test(new RhinoProxy());
	}

	private void test(JSProxy p) {
		p.eval("var a = 1;");
		Map<String, Object> varMap=new HashMap<String, Object>();
		varMap.put("b", 2.0);
		
		Assert.assertEquals("4",p.eval("a = a+1;String(a +b)",".",varMap));
		Assert.assertEquals("1",p.eval("String(a)",".",varMap));

		p.createTextParser(p.eval("(function(){})"));
		p.createNodeParser(p.eval("(function(){})"));
		//p.eval("this.a=5",".",varMap);
		//Assert.assertEquals("5",p.eval("String(a)","."));
	}

}
