package org.xidea.el.test;

import org.junit.Before;
import org.junit.Test;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;

public class AutoTest {
	ExpressionFactory expressionFactory = ExpressionFactoryImpl.getInstance();



	@Before
	public void setup(){
		
	}
	@Test
	public void testAll() throws Exception {
		ELTest.testEL("{\"a\":1}","a+1");
		
	}
}
