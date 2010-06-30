package org.xidea.el.parser.test;

import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;


import org.junit.Assert;
import org.junit.Test;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.ExpressionToken;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.impl.ExpressionTokenizer;
import org.xidea.el.impl.TokenImpl;

public class OperatorAliasTest {
	ExpressionFactoryImpl ef = new ExpressionFactoryImpl()
	{
		private HashMap<String, Integer> operatorAliasMap = new HashMap<String, Integer>();
		{
			addOperatorAlias("!","not");//取非
			addOperatorAlias(">", "gt");//大于;
			addOperatorAlias("<","lt");//小于
			addOperatorAlias(">=","ge");//大于等于
			addOperatorAlias("<=","le");//小于等于
			addOperatorAlias("==","eq");//等于
			addOperatorAlias("!=","ne");//不等于
			addOperatorAlias("/","div");//除
			addOperatorAlias("%","mod");//取余数
			addOperatorAlias("&&","and");//且
			addOperatorAlias("||","or");//或
		}
		public void addOperatorAlias(Object op,String... alias){
			if(op instanceof String){
				op = new TokenImpl((String)op).getType();
			}
			if(op!= null){
				for(String as:alias){
					this.operatorAliasMap.put(as,(Integer)op);
				}
			}
			
		}
		public Object parse(String el) {
			ExpressionToken tokens = new ExpressionTokenizer(el, operatorAliasMap)
					.getResult();
			return tokens;
		}
	};
	@Test
	public void testNot()throws Exception {
		test(false, "not true");//!true
		test(false, "not not not (1 and 1)");//!true
		test(false, "not [(1 eq 1)][0]");//!true
		test(false, "not {a:(1 ge 1)}['a']");//!true
	}
	@Test
	public void testCompare()throws Exception {
		test(false, "false eq true");//!true
		test(true, "1 gt 0");//!true
		test(false, "1 lt 1");//!true
		test(false, "1 ge 2");//!true
	}

	@Test
	public void testAndOr()throws Exception {
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
