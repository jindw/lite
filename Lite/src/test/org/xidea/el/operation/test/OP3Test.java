package org.xidea.el.operation.test;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.impl.ExpressionImpl;
import org.xidea.lite.parser.impl.JSProxy;

public class OP3Test {
	@Test
	public void testEQ() {
		System.out.println(true?true:true?false:false);
		doTest("1?1:3 + 0?5:7");//1?1:(3+0?5:7)
		doTest("1?3 + 0?5:7:1");//1?(3 + 0?5:7):1
		doTest("1?3 + 0?5:7:23 + 0?511:711");//[1?3 + 0?5:7:23 + 0?511:711,1?(3 + 0?5:7):(23 + 0?511:711)]1?(3 + 0?5:7):1
		doTest("3 + 0?5:7?23:1");//[3 + 0?5:7?23:1,(3 + 0)?5:(7?23:1)]
		doTest("-1 + +1?5:7?23:1");//[-1 + +1?5:7?23:1,(-1 +1)?5:(7?23:1)]
		doTest("1?0?5:7:3 ");
		doTest("0?0?5:7:3 ");
		doTest("1?0?5:0?11:13:3");
		doTest("1?1?0?5:0?11:13:3?1?0?5:0?11:13:3:0?11:13:3");
		doTest("1>1?1:2");
		doTest("1>=1?1:2");
		doTest("1<1?1:2");
		doTest("1<=1?1:2");

		doTest("-1>0?1:2");
		doTest("-1>=0?1:2");
		doTest("-1<0?1:2");
		doTest("-1<=0?1:2");
	}

	private void doTest(String el) {
		Float actual = ((Number) ExpressionFactoryImpl.getInstance().create(el).evaluate("")).floatValue();
		System.out.println("acture"+actual);
		Float expected = ((Number)JSProxy.newProxy().eval(el)).floatValue();
		Assert.assertEquals(el, (expected),(actual));
	}
}
