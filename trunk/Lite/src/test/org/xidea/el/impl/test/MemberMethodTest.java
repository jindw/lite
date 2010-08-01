package org.xidea.el.impl.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;


public class MemberMethodTest {
	ExpressionFactory factory = new ExpressionFactoryImpl();

	@Test
	public void testEncodeURLComponentEL() throws Exception{
		System.out.println( factory.create("(123.4.intValue()+11)").evaluate());
		Expression el = factory.create("(1234).intValue() == 1234");
		Expression el2 = factory.create("(1234.intValue()) == 1234");
		assertEquals(true, el.evaluate());
		assertEquals(true, el2.evaluate());
	}
	
}
