package org.xidea.el.impl.test;

import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;

public class ValidExpresionTest {
	ExpressionFactory expressionFactory = ExpressionFactoryImpl.getInstance();

	public void invalid(String el) throws Exception {
		try {
			expressionFactory.create(el);
			expressionFactory.parse(el);
			Assert.fail("必须抛ExpressionSyntaxException异常");
		} catch (org.xidea.el.ExpressionSyntaxException e) {
		} catch (java.lang.AssertionError e) {
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
			Assert.fail("只能是抛ExpressionSyntaxException异常:"+e.getMessage());
		}
	}
	public void valid(String el) throws Exception {
		try {
			expressionFactory.create(el);
			expressionFactory.parse(el);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("检查失败"+e.getMessage());
		}
	}
	public void testOptimizeELString() {
		expressionFactory.parse("''");
		expressionFactory.parse("'['");
		try{
			expressionFactory.parse("'''");
			fail("无效字符串状态");
		}catch (Exception e) {
		}

		try{
			expressionFactory.parse("[([)]]");
			fail("无效括弧状态");
		}catch (Exception e) {
		}
	}

	@Test
	public void testEL1() throws Exception {
		invalid("1+");
		invalid("1;+1");
	}
	@Test
	public void testEL2() throws Exception {
		valid("{}");
		valid("[{}]");
	}
}
