package org.xidea.el.impl.test;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.xidea.el.ExpressionToken;
import org.xidea.el.impl.ExpressionFactoryImpl;

@SuppressWarnings("unchecked")
public class UserDefinedOperatorTest {
	public static boolean in(Object key,Map map){
		return map.containsKey(key);
	}
	public static boolean contains(List list,Object key){
		return list.contains(key);
	}
	ExpressionFactoryImpl ef = new ExpressionFactoryImpl();
	{
		ef.addOperator(ExpressionToken.OP_NOT,"not",null);//取非
		ef.addOperator(ExpressionToken.OP_GT, "gt",null);//大于;
		ef.addOperator(ExpressionToken.OP_LT,"lt",null);//小于
		ef.addOperator(ExpressionToken.OP_GTEQ,"ge",null);//大于等于
		ef.addOperator(ExpressionToken.OP_LTEQ,"le",null);//小于等于
		ef.addOperator(ExpressionToken.OP_EQ,"eq",null);//等于
		ef.addOperator(ExpressionToken.OP_NE,"ne",null);//不等于
		ef.addOperator(ExpressionToken.OP_DIV,"div",null);//除
		ef.addOperator(ExpressionToken.OP_MOD,"mod",null);//取余数
		ef.addOperator(ExpressionToken.OP_AND,"and",null);//且
		ef.addOperator(ExpressionToken.OP_OR,"or",null);//或
		
		//自定义操作符号
		try {
			//in
			ef.addOperator(ExpressionToken.OP_GT,"in",UserDefinedOperatorTest.class.getMethod("in", Object.class,Map.class));
			ef.addOperator(ExpressionToken.OP_GT,"contains",UserDefinedOperatorTest.class.getMethod("contains",List.class, Object.class));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	};
	@Test
	public void testUserDefined()throws Exception {
		test(true, "'a' in {a:0}");
		test(false, "'a2' in {a:0}");
		test(false, "['a1','a2'] contains 'a'");
		test(true, "['a1','a','a2'] contains 'a'");
	}
	@Test
	public void testAlias()throws Exception {
		test(false, "not true");//!true
		test(false, "not not not (1 and 1)");//!true
		test(false, "not [(1 eq 1)][0]");//!true
		test(false, "not {a:(1 ge 1)}['a']");//!true
		
		test(false, "false eq true");//!true
		test(true, "1 gt 0");//!true
		test(false, "1 lt 1");//!true
		test(false, "1 ge 2");//!true
		
		test(false, "false and true");//!true
		test(true, "false or true");//!true
	}
	
	public void test(Object expect,String el, Object ...context){
		Object result = eval(el);
		Assert.assertEquals(expect, result);
	}
	private Object eval(String el,Object... context){
		return ef.create(el).evaluate(context);
	}

}
