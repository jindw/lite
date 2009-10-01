package org.xidea.el.test;

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
		Assert.assertEquals("[98,[33,[-2,\"object\"],null,\"test\"],[64,[-3],[-1,123]]]", el);
	}

}
