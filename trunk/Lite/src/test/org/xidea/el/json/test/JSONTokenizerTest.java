package org.xidea.el.json.test;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.jsi.impl.v3.RuntimeSupport;

public class JSONTokenizerTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testFloat(){
		doTest("67E+12-34");
		doTest("067");
		doTest("0x67E12");
		doTest("67E+1");
		doTest("67E-1");
		doTest("67E+12");
		doTest("67E-12");
		//new JSONTokenizer("0xfff2ed /19.5e-2+ 2 +19.5E-2").parse();
		doTest("0xfff2ed /19.5e-2+ 2 +19.5E-2");
		doTest("(19E2)");
		doTest("(0xCCFF%2)+(0676/(19.5E-2)-(19.5E-2))*(0676/(19.5E-2)-(19.5E-2))");
		System.out.println(010);
		doTest("0676/(19.5E-2)-(19.5E-2)");
		
	}

	private void doTest(String el) {
		Float actual = ((Number) ExpressionFactoryImpl.getInstance().create(el).evaluate("")).floatValue();
		System.out.println("acture"+actual);
		Float expected = ((Number)RuntimeSupport.create().eval(el)).floatValue();
		Assert.assertEquals( toInt(expected),toInt(actual));
	}

	private int toInt(Float expected) {
		return new Float(expected*1000).intValue();
	}
}
