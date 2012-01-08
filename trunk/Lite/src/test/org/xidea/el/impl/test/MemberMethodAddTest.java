package org.xidea.el.impl.test;


import org.junit.Assert;
import org.junit.Test;
import org.xidea.el.Expression;
import org.xidea.el.Invocable;
import org.xidea.el.impl.ExpressionFactoryImpl;


public class MemberMethodAddTest {
	ExpressionFactoryImpl factory = new ExpressionFactoryImpl();

	public Object getStringArray(){
		return new String[]{"1","2"};
	}
	@Test
	public void testAddStaticMethod() throws Exception{
		Expression el = factory.create("stringArray.add2(123)");
		factory.addMethod(Object[].class, "add2",
				new Invocable() {
					public Object invoke(Object thiz, Object... args) throws Exception {
						StringBuffer buf = new StringBuffer();
						for(Object o  : (Object[])thiz){
							buf.append(args[0]);
							buf.append(o);
						}
						return buf.toString();
					}
				});
		Assert.assertEquals("12311232", el.evaluate(this));
	}
	
}
