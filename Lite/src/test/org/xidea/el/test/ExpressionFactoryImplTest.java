package org.xidea.el.test;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.xidea.el.ExpressionFactoryImpl;

public class ExpressionFactoryImplTest {
	ExpressionFactoryImpl expressionFactory = new ExpressionFactoryImpl();
	@Test
	public void testOptimizeELString() {
		System.out.println((ExpressionFactoryImplTest.class.getInterfaces()));
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

}
