package org.xidea.el.impl.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;


public class JSArrayTest {
	ExpressionFactory factory = new ExpressionFactoryImpl();
/**

			"slice,splice,concat,join"+
			//4-7
			",push,pop,shift,unshift"+
			//8-9
			",reverse,sort";
 */
	@Test
	public void testArray() throws Exception{
		testELValue("[1,2,3,4].toArray().slice(1,-1).join('-')","2-3");
		testELValue("[1,2,3,4].slice(1,-1).join('-')","2-3");
		testELValue("[1,2,3,4].reverse(5).join(',')","4,3,2,1");
		testELValue("[1,2,3,4].push(5,4)",6);
		testELValue("[1,2,3,4].pop(5)",4);
		testELValue("[1,2,3,4].join('-')","1-2-3-4");
		testELValue("[1,2,3,4].slice(1,3).join('-')","2-3");
	}
	public void testELValue(String exp,Object value) throws Exception{
		Expression el = factory.create(exp);
		assertEquals(value, el.evaluate());
	}
	
}
