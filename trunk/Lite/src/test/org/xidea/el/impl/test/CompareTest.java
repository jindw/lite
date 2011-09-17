package org.xidea.el.impl.test;

import junit.framework.Assert;

import org.junit.Test;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;

public class CompareTest {
	ExpressionFactory ef = ExpressionFactoryImpl.getInstance();
	@Test
	public void test(){
		String el = "NaN==NaN";
		System.out.println(ef.create(el).evaluate());
	}
	
	@Test
	public void testNumberCompare(){
		test("2>1",true);
		test("2>=1",true);
		test("2<1",false);
		test("2<=1",false);
		test("2==1",false);
		

		test("1>2",false);
		test("1>=2",false);
		test("1<2",true);
		test("1<=2",true);
		test("1==2",false);

		test("1>1",false);
		test("1>=1",true);
		test("1<1",false);
		test("1<=1",true);
		test("1==1",true);

		test("NaN==NaN",false);
		test("NaN!=NaN",true);
		test("Infinity>=NaN",false);
		//[1>1
		//,1&lt;2,
		//1>=1,
		//1&lt;=0,
		//1>=NaN,
		//NaN==NaN,Infinity>Infinity,Infinity>NaN
		
	}

	private void test(String el, boolean expected) {
		boolean actual = (Boolean)ef.create(el).evaluate();
		Assert.assertEquals(el,expected, actual);
	}

}
