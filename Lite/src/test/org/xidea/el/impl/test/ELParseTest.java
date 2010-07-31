package org.xidea.el.impl.test;

import org.junit.Assert;
import org.junit.Test;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.json.JSONEncoder;

public class ELParseTest {
	private ExpressionFactoryImpl ef = new ExpressionFactoryImpl();
	@Test
	public void testMethod(){
		Object el = ef.parse("object.test(123)");
		el = JSONEncoder.encode(el);
		Assert.assertEquals("[97,[96,[-2,\"object\"],[-1,\"test\"]],[64,[-3],[-1,123]]]", el);
	}

	@Test
	public void test3op(){
		System.out.println(JSONEncoder.encode( ef.parse("0?0?5:7:3")));
		System.out.println(JSONEncoder.encode( ef.parse("0?(0?5:7):3")));
	}

}
