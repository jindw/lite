package org.xidea.el.operation.test;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Test;
import org.xidea.el.impl.ExpressionImpl;

public class EQTest {
	@Test
	public void testEQ() {
		HashMap<?, ?> context = new HashMap<Object, Object>(){
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public String toString(){
				return "v"+System.currentTimeMillis();
			}
		};
		assertEquals(true, new ExpressionImpl("'123' == 123").evaluate(context));
		assertEquals(true, new ExpressionImpl("this == this").evaluate(context));
		assertEquals(true, new ExpressionImpl("this != this+1").evaluate(context));
	}

}
