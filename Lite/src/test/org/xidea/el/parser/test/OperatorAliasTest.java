package org.xidea.el.parser.test;

import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.ExpressionToken;
import org.xidea.el.impl.ExpressionFactoryImpl;

public class OperatorAliasTest {
	ExpressionFactory ef = ExpressionFactoryImpl.getInstance();
	@Test
	public void testNot()throws Exception {
		test(false, "not true");//!true
		test(false, "not not not (1 and 1)");//!true
		test(false, "not [(1 eq 1)][0]");//!true
		test(false, "not {a:(1 ge 1)}['a']");//!true
	}
	@Test
	public void testCompare()throws Exception {
		test(false, "false eq true");//!true
		test(true, "1 gt 0");//!true
		test(false, "1 lt 1");//!true
		test(false, "1 ge 2");//!true
	}

	@Test
	public void testAndOr()throws Exception {
		test(false, "false and true");//!true
		test(true, "false or true");//!true
	}
	
	public void test(Object expect,String el, Object ...context){
		Object result = eval(el);
		Assert.assertEquals(expect, result);
	}
	private Object eval(String el,Object... context){
		return ef.create(el).evaluate(context);
	}

}
