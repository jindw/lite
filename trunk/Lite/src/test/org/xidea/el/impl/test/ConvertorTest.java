package org.xidea.el.impl.test;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.xidea.el.impl.Convertor;


public class ConvertorTest {
	Map<Object, Object> context = new HashMap<Object, Object>();
	@Test
	public void test(){
		System.out.println(Character.isTitleCase('。'));
		System.out.println(Character.isTitleCase('．'));
		System.out.println(Long.class.isAssignableFrom(Long.class));
		System.out.println(Long.TYPE.isAssignableFrom(Long.class));
		System.out.println(Number.class.isAssignableFrom(int.class));
		System.out.println(Integer.TYPE == Integer.class);
		System.out.println(Integer.TYPE.isAssignableFrom(Integer.class));
		System.out.println(Integer.class.isAssignableFrom(Integer.TYPE));
	}
		@Test
		public void testDefault(){
		String value = "123";
		doTest(value,Integer.TYPE,123);
		doTest(value,Integer.class,123);
		doTest(value,Double.TYPE,123d);
		doTest(null,Double.TYPE,0d);
		doTest(null,Double.class,null);
		doTest(value,Boolean.TYPE,true);
		doTest(value,Boolean.class,true);
		doTest("",Boolean.TYPE,false);
		doTest("0",Boolean.class,false);
		doTest("false",Boolean.class,false);
		doTest("false",Object.class,"false");
		doTest("false",String.class,"false");
		doTest("false",String.class.getSuperclass(),"false");
		doTest("false",this.getClass(),null);
		doTest("D"+value,Double.TYPE,0d);
		doTest("D"+value,Double.class,null);
	}
	private void doTest(String value, Class<?> type, Object expected) {
		Convertor c = Convertor.DEFAULT;
		Object result = c.getValue(value, type, context, "key");
		Assert.assertEquals(expected, result);
		if(result != null){
		System.out.println(result +"/"+ (result == null?null:result.getClass()));
		Assert.assertEquals(expected == null?Object.class:expected.getClass(), result.getClass());
		}
	}

}
