package org.xidea.el.operation.test;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Test;
import org.xidea.el.impl.ExpressionImpl;

public class OP3Test {
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
		assertEquals(2, new ExpressionImpl("1>1?1:2").evaluate(context));
		assertEquals(1, new ExpressionImpl("1>=1?1:2").evaluate(context));
		assertEquals(2, new ExpressionImpl("1<1?1:2").evaluate(context));
		assertEquals(1, new ExpressionImpl("1<=1?1:2").evaluate(context));

		assertEquals(2, new ExpressionImpl("-1>0?1:2").evaluate(context));
		assertEquals(2, new ExpressionImpl("-1>=0?1:2").evaluate(context));
		assertEquals(1, new ExpressionImpl("-1<0?1:2").evaluate(context));
		assertEquals(1, new ExpressionImpl("-1<=0?1:2").evaluate(context));
	}

}
