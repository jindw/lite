package org.xidea.el.impl.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.ExpressionToken;
import org.xidea.el.fn.ECMA262Impl;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.impl.OperationStrategyImpl;
import org.xidea.el.impl.OptimizeExpressionImpl;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.test.ELTest;

public class OptimizeExpressionImplTest {

	OperationStrategyImpl strategy = new OperationStrategyImpl(false);
	{
		ECMA262Impl.setup(new ExpressionFactoryImpl(strategy));
	}
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCreate() {
		doTest("變量獲取","1","o","{\"o\":\"1\"}");
		doTest("屬性獲取","1","o.a","{\"o\":{\"a\":\"1\"}}");
		doTest("多重屬性獲取","2","o.a.b","{\"o\":{\"a\":{\"b\":\"2\"}}}");
	}

	private void doTest(String msg,Object expected,String el,String context) {
		ELTest.testEL(context, el,false);
		ExpressionFactory ef = ExpressionFactoryImpl.getInstance();
		Expression exp = OptimizeExpressionImpl.create((ExpressionToken)ef.parse(el),ef,  strategy);
		Assert.assertTrue("不需是有效的優化表達式/"+msg,exp instanceof OptimizeExpressionImpl);
		Assert.assertEquals(msg,expected, exp.evaluate(JSONDecoder.decode(context)));
	}

}
