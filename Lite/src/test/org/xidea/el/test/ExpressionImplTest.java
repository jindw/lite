package org.xidea.el.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.xidea.el.ExpressionImpl;

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
	private void test(Map<String, Object> context,String el,Object expected, boolean exact){
		Object result = eval(el,context);
		assertEquals(expected, result);
		if(result!=null && exact){
			System.err.println("result Type:"+expected.getClass()+result.getClass());
			//assertEquals(expected.getClass(), result.getClass());
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
		
		test(numberContext,"1+f1",2f, true);
	}

	@Test
	public void testIntMath() {
		test(null,"1+2",3, true);
		test(null,"1/2",0.5, true);
		test(null,"1+2*2",5, true);
		test(null,"(1+2)*2",6, true);
		test(null,"(1-2)*2",-2, true);
		test(null,"1+2 == '3'",true, true);
	}
	@Test
	public void test3op() {	
		test(null,"(0?1+4:+2)+1",3, true);

		test(null,"0?1:2",2, true);
		test(null,"0?1+4:2*2",4, true);
		test(null,"0?1+4:+2",2, true);
	}
	@Test
	public void testProp() {
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("var1", Arrays.asList(1,2));
		test(context,"var1[0]+1+var1[1]",4, true);
	}
	@Test
	public void testListConstructor() {
		Map<String, Object> context = new HashMap<String, Object>();
		test(context,"[1,2,3][1]",2, true);
		test(context,"[]",new ArrayList<Object>(), true);
	}
	@Test
	public void testMapConstructor() {
		Map<String, Object> context = new HashMap<String, Object>();
		test(context,"{}",new LinkedHashMap<Object, Object>(), true);
		test(context,"{aaa:1,'bb':2}['aaa']",1, true);
		test(context,"{aaa:1,bb:2}['bb']",2, true);
	}
	@Test
	public void testListMap() {
		Map<String, Object> context = new HashMap<String, Object>();
		test(context,"{aaa:1,'bb':[1,3,2]}['bb'][0]",1, true);
		test(context,"{aaa:1,'bb':[1,3,2]}['bb']['1']",3, true);
		test(context,"[1,{aa:2}][1]['aa']",2, true);
	}
	@Test
	public void testMethod() {
		Map<String, Object> context = new HashMap<String, Object>();
		test(context,"'123'.startsWith('12')",true, true);
		test(context,"'123'.endsWith('12')",false, true);
	}

	@Test
	public void testRelative() {
		System.out.println(Double.NaN>0);
		System.out.println(Double.NaN<0);
		System.out.println(Double.NaN==0);
		System.out.println(new Double(0).compareTo(Double.NaN));
		System.out.println(new Double(Double.NaN).compareTo(0d));
		//System.out.println(Integer.parseInt("0xFFFFFFFFFFFF"));
		test(null,"1!=2",true, true);
		test(null,"1==2",false, true);
		test(null,"1>2",false, true);
		test(null,"1>=2",false, true);
		test(null,"1<=2",true, true);
		test(null,"2==2",true, true);
		test(null,"1<2",true, true);
	}
}
