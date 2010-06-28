package org.xidea.el.test;

import org.junit.Assert;
import org.junit.Test;
import org.xidea.el.Expression;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.parser.impl.JSProxy;

public class ELParseTest {
	private ExpressionFactoryImpl ef = new ExpressionFactoryImpl();
	@Test
	public void testMethod(){
		Object el = ef.parse("object.test(123)");
		el = JSONEncoder.encode(el);
		Assert.assertEquals("[98,[33,[-2,\"object\"],null,\"test\"],[64,[-3],[-1,123]]]", el);
	}

	@Test
	public void test3op(){
		System.out.println(JSONEncoder.encode( ef.parse("0?0?5:7:3")));
		System.out.println(JSONEncoder.encode( ef.parse("0?(0?5:7):3")));
		testEL("1?1:3 + 0?5:7");
		testEL("1?0?5:7:3 ");
		testEL("0?0?5:7:3 ");
		testEL("1?0?5:0?11:13:3");
		testEL("1?1?0?5:0?11:13:3?1?0?5:0?11:13:3:0?11:13:3");
	}

	@Test
	public void testSimple(){
		testEL("(-123).toFixed(2)");
	}
	private void testEL(String elt) {
		Expression el = ef.create(elt);
		System.out.println(JSONEncoder.encode(elt)+el.evaluate(""));
		Assert.assertEquals(JSProxy.newProxy().eval(elt),el.evaluate(""));
	}
}
