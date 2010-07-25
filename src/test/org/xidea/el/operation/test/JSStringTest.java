package org.xidea.el.operation.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;


import org.junit.Test;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;


public class JSStringTest {
	ExpressionFactory factory = new ExpressionFactoryImpl();
	@Test
	public void testString() throws Exception{
		testELValue("\"abcdefg\".indexOf('g')",6);
		testELValue("\"abcdefg\".replace(\"a\", \"1\")","1bcdefg");
		testELValue("'abcdefg'.charAt(1)",'b');
	}
	public void testELValue(String exp,Object value) throws Exception{
		Expression el = factory.create(exp);
		assertEquals(value, el.evaluate(null));
	}
	
}
