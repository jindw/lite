package org.xidea.el.impl.test;

import junit.framework.Assert;

import org.junit.Test;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.jsi.JSIRuntime;
import org.xidea.jsi.impl.RuntimeSupport;

public class QuestionTest {
	ExpressionFactory ef = ExpressionFactoryImpl.getInstance();
	JSIRuntime rt = RuntimeSupport.create();

	@Test
	public void testRandom() {
		for (int i = 0; i < 1000; i++) {
			test(r3p(0.1,0));
		}
	}

	private String r3p(double gtis3op,int depth) {
		double r = Math.random();
		if (r > gtis3op * depth) {
			StringBuffer buf = new StringBuffer();
			buf.append(rb());
			buf.append('?');
			buf.append(r3p(gtis3op,depth+1));//
			buf.append(':');
			buf.append(r3p(gtis3op,depth+1));//
			return buf.toString();
		} else {
			return "" + (int) (r * 10000);
		}
	}

	private boolean rb() {
		return Math.random() > 0.5;
	}

	private void test(String el) {
		System.out.println("test:" + el);
		Number actual = (Number) ef.create(el).evaluate();
		Number expected = (Number) rt.eval(el);
		Assert.assertEquals(el, expected.intValue(), actual.intValue());
	}

}
