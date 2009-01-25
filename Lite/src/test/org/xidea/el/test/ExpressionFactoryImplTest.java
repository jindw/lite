package org.xidea.el.test;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.xidea.el.ExpressionFactoryImpl;

public class ExpressionFactoryImplTest {
	ExpressionFactoryImpl expressionFactory = new ExpressionFactoryImpl();
	@Test
	public void testOptimizeELString() {
		System.out.println((ExpressionFactoryImplTest.class.getInterfaces()));
		expressionFactory.optimizeEL("''");
		expressionFactory.optimizeEL("'['");
		try{
			expressionFactory.optimizeEL("'''");
			fail("无效字符串状态");
		}catch (Exception e) {
		}

		try{
			expressionFactory.optimizeEL("[([)]]");
			fail("无效括弧状态");
		}catch (Exception e) {
		}
	}

}
