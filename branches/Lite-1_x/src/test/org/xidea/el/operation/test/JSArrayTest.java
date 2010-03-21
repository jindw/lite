package org.xidea.el.operation.test;

import static org.junit.Assert.assertEquals;


import org.junit.Test;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;


public class JSArrayTest {
	ExpressionFactory factory = new ExpressionFactoryImpl();

	@Test
	public void testArray() throws Exception{
		testELValue("[1,2,3,4].join('-')","1-2-3-4");
		testELValue("[1,2,3,4].slice(1,3).join('-')","2-3");
	}
	public void testELValue(String exp,String value) throws Exception{
		Expression el = factory.create(exp);
		assertEquals(value, el.evaluate(null));
	}
	
}
