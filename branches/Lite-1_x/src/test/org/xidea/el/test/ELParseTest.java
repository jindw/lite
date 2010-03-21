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
		Assert.assertEquals("[[-1,\"object\"],[48,\"test\"],[-3],[0,123],[1],[81]]", el);
		//[[-1,"object"],[48,"test"],[-3],[0,123],[1,None],[81]]
	}

}
