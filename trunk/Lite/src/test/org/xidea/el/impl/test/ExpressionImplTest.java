package org.xidea.el.impl.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.el.impl.ExpressionImpl;

public class ExpressionImplTest {
	private Map<String, Object> numberContext = new HashMap<String, Object>();
	{
		numberContext.put("d1", 1d);
		numberContext.put("f1", 1f);
		numberContext.put("l1", 1l);
		numberContext.put("i1", 1);
		numberContext.put("s1", (short)1);
		numberContext.put("b1", (byte)1);
	}

	@Before
	public void setUp() throws Exception {
	}
	private Object eval(String el,Map<String, Object> context) {
		ExpressionImpl exp = new ExpressionImpl(el);
		Object result = exp.evaluate(context);
		return result;
	}
	private void test(Map<String, Object> context,String el, Class<? extends Object> checkType){
		Object result = eval(el,context);
		if(checkType!=null && result!=null){
			Assert.assertTrue("返回值类型不匹配："+checkType+"!="+result.getClass()+"#"+result,checkType.isInstance(result));
		}
	}
	@Test
	@SuppressWarnings("unused")
	public void testMathType(){
		//test(numberContext,"1+2.0",3d, true);
		//double b = 1l+2d;
		long l = 1;
		int i = 1;
		short s = 1;
		byte b=1;
		double d=1;
		float f = 1;
		
		test(numberContext,"1+f1", Float.class);
	}

	@Test
	public void testIntMath() {
		test(null,"1+2",Long.class);
		test(null,"1/2",Double.class);
		test(null,"1+2*2",Long.class);
		test(null,"(1+2)*2",Long.class);
		test(null,"(1-2)*2",Long.class);
		test(null,"1+2 == '3'",Boolean.class);
	}
	@Test
	public void test3opAndFN() {	
		test(null,"Math.max(2>=0?3+2:2,1)",Long.class);
	}
	@Test
	public void testNotNot() {	
		test(null,"!!1",Boolean.class);
		test(null,"!!1",Boolean.class);
		test(null,"!![1][0]",Boolean.class);
		test(null,"!!''.length",Boolean.class);
	}
	@Test
	public void test3op1() {	
		test(null,"0?1:2",Long.class);
	}
	@Test
	public void test3op2() {	
			test(null,"(0?1+4:+2)+1",Long.class);

		test(null,"0?1:2",Long.class);
		test(null,"0?1+4:2*2",Long.class);
		test(null,"0?1+4:+2",Long.class);
	}
	@Test
	public void testProp() {
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("var1", Arrays.asList(1,2));
		test(context,"var1[0]+1+var1[1]",Long.class);
	}
	@Test
	public void testListConstructor() {
		Map<String, Object> context = new HashMap<String, Object>();
		test(context,"[1,2,3][1]",Long.class);
		test(context,"[]",List.class);
	}
	@Test
	public void testMapConstructor() {
		Map<String, Object> context = new HashMap<String, Object>();
		test(context,"{}",LinkedHashMap.class);
		test(context,"{aaa:1,'bb':2}['aaa']",Long.class);
		test(context,"{aaa:1,bb:2}['bb']",Long.class);
	}
	@Test
	public void testListMap() {
		Map<String, Object> context = new HashMap<String, Object>();
		test(context,"{aaa:1,'bb':[1,3,2]}['bb'][0]",Long.class);
		test(context,"{aaa:1,'bb':[1,3,2]}['bb']['1']",Long.class);
		test(context,"[1,{aa:2}][1]['aa']",Long.class);
	}
	@Test
	@Deprecated
	public void testMethod() {
		Map<String, Object> context = new HashMap<String, Object>();
		test(context,"'123'.startsWith('12')",Boolean.class);
		test(context,"'123'.endsWith('12')",Boolean.class);
	}

	@Test
	public void testRelative() {
		System.out.println(Double.NaN>0);
		System.out.println(Double.NaN<0);
		System.out.println(Double.NaN==0);
		System.out.println(new Double(0).compareTo(Double.NaN));
		System.out.println(new Double(Double.NaN).compareTo(0d));
		//System.out.println(Long.parseInt("0xFFFFFFFFFFFF"));
		test(null,"1!=2",Boolean.class);
		test(null,"1==2",Boolean.class);
		test(null,"1>2",Boolean.class);
		test(null,"1>=2",Boolean.class);
		test(null,"1<=2",Boolean.class);
		test(null,"2==2",Boolean.class);
		test(null,"1<2",Boolean.class);
	}
}
